package prerna.sablecc2.reactor.project;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityProjectUtils;
import prerna.auth.utils.SecurityQueryUtils;
import prerna.date.SemossDate;
import prerna.engine.api.IRawSelectWrapper;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.util.Constants;
import prerna.util.sql.AbstractSqlQueryUtil;

public class MyProjectsReactor extends AbstractReactor {
	
	private static final Logger logger = LogManager.getLogger(MyProjectsReactor.class);

	private static List<String> META_KEYS_LIST = new Vector<>();
	static {
		META_KEYS_LIST.add("description");
		META_KEYS_LIST.add("tag");
	}

	public MyProjectsReactor() {
		this.keysToGet = new String[] {ReactorKeysEnum.ONLY_FAVORITES.getKey(), ReactorKeysEnum.SORT.getKey()};
	}

	@Override
	public NounMetadata execute() {
		organizeKeys();
		Boolean favoritesOnly = Boolean.parseBoolean(this.keyValue.get(this.keysToGet[0]));
		String sortCol = this.keyValue.get(this.keysToGet[1]);
		if(sortCol == null) {
			sortCol = "name";
		}
		
		List<Map<String, Object>> projectInfo = new Vector<>();

		if(AbstractSecurityUtils.securityEnabled()) {
			projectInfo = SecurityProjectUtils.getUserProjectList(this.insight.getUser(), favoritesOnly);
			this.insight.getUser().setEngines(projectInfo);
		} else {
			projectInfo = SecurityProjectUtils.getAllProjectList();
		}

		int size = projectInfo.size();
		Map<String, Integer> index = new HashMap<>(projectInfo.size());
		// now we want to add most executed insights
		for(int i = 0; i < size; i++) {
			Map<String, Object> project = projectInfo.get(i);
			String appId = project.get("project_id").toString();
			SemossDate lmDate = SecurityQueryUtils.getLastModifiedDateForInsightInProject(appId);
			// could be null when there are no insights in an app
			if(lmDate != null) {
				project.put("lastModifiedDate", lmDate);
				project.put("lastModified", lmDate.getFormattedDate());
			}

			// just going to init for FE
			project.put("description", "");
			project.put("tags", new Vector<String>());

			// keep list of app ids to get the index
			index.put(appId, Integer.valueOf(i));
		}

		IRawSelectWrapper wrapper = null;
		try {
			wrapper = SecurityProjectUtils.getProjectMetadataWrapper(index.keySet(), META_KEYS_LIST);
			while(wrapper.hasNext()) {
				Object[] data = wrapper.next().getValues();
				String projectId = (String) data[0];

				String metaKey = (String) data[1];
				String value = AbstractSqlQueryUtil.flushClobToString((java.sql.Clob) data[2]);
				if(value == null) {
					continue;
				}

				int indexToFind = index.get(projectId);
				Map<String, Object> res = projectInfo.get(indexToFind);
				// right now only handling description + tags
				if(metaKey.equals("description")) {
					// we only have 1 description per insight
					// so just push
					res.put("description", value);
				} else if(metaKey.equals("tag")){
					// multiple tags per insight
					List<String> tags = (List<String>) res.get("tags");;
					// add to the list
					tags.add(value);
				}
			}
		} catch (Exception e) {
			logger.error(Constants.STACKTRACE, e);
		} finally {
			if(wrapper != null) {
				wrapper.cleanUp();
			}
		}
		
		// need to go through and sort the values
		if(sortCol.equalsIgnoreCase("date")) {
			Collections.sort(projectInfo, new Comparator<Map<String, Object>>() {

				// we want descending - not ascending
				// so values are swapped
				@Override
				public int compare(Map<String, Object> o1, Map<String, Object> o2) {
					SemossDate d1 = (SemossDate) o1.get("lastModifiedDate");
					SemossDate d2 = (SemossDate) o2.get("lastModifiedDate");
					
					if(d1 == null) {
						return 1;
					}
					if(d2 == null) {
						return -1;
					}

					return d2.compareTo(d1);
				}
			});
		} else {
			Collections.sort(projectInfo, new Comparator<Map<String, Object>>() {

				// we want descending - not ascending
				// so values are swapped
				@Override
				public int compare(Map<String, Object> o1, Map<String, Object> o2) {
					String name1 = (String) o1.get("low_project_name");
					String name2 = (String) o2.get("low_project_name");
					
					return name1.compareTo(name2);
				}
			});
		}

		return new NounMetadata(projectInfo, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.DATABASE_INFO);
	}

	@Override
	protected String getDescriptionForKey(String key) {
		if(key.equals(ReactorKeysEnum.SORT.getKey())) {
			return "The sort is a string value containing either 'name' or 'date' for how to sort";
		}
		return super.getDescriptionForKey(key);
	}
}

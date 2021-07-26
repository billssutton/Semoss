package prerna.sablecc2.reactor.insights.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.auth.AccessToken;
import prerna.auth.User;
import prerna.auth.utils.AbstractSecurityUtils;
import prerna.auth.utils.SecurityInsightUtils;
import prerna.auth.utils.SecurityProjectUtils;
import prerna.cache.InsightCacheUtility;
import prerna.cluster.util.ClusterUtil;
import prerna.engine.impl.InsightAdministrator;
import prerna.om.PixelList;
import prerna.project.api.IProject;
import prerna.query.parsers.ParamStruct;
import prerna.sablecc2.PixelUtility;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.PixelOperationType;
import prerna.sablecc2.om.ReactorKeysEnum;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.insights.AbstractInsightReactor;
import prerna.util.AssetUtility;
import prerna.util.Constants;
import prerna.util.MosfetSyncHelper;
import prerna.util.Utility;
import prerna.util.git.GitRepoUtils;
import prerna.util.git.GitUtils;
import prerna.util.insight.InsightUtility;

public class SaveInsightReactor extends AbstractInsightReactor {

	private static final Logger logger = LogManager.getLogger(SaveInsightReactor.class);
	private static final String CLASS_NAME = SaveInsightReactor.class.getName();
	
	public SaveInsightReactor() {
		this.keysToGet = new String[]{ReactorKeysEnum.PROJECT.getKey(), ReactorKeysEnum.INSIGHT_NAME.getKey(), 
				ReactorKeysEnum.LAYOUT_KEY.getKey(), HIDDEN_KEY, ReactorKeysEnum.RECIPE.getKey(), 
				ReactorKeysEnum.PARAM_KEY.getKey(), ReactorKeysEnum.DESCRIPTION.getKey(), 
				ReactorKeysEnum.TAGS.getKey(), ReactorKeysEnum.IMAGE.getKey(), ENCODED_KEY};
	}

	@Override
	public NounMetadata execute() {
		Logger logger = this.getLogger(CLASS_NAME);
		// get the recipe for the insight
		// need the engine name and id that has the recipe
		boolean savingThisInsight = false;
		boolean optimizeRecipe = true;
		String projectId = getProject();
		User user = this.insight.getUser();
		String author = null;
		String email = null;
		
		// security
		if(AbstractSecurityUtils.securityEnabled()) {
			if(AbstractSecurityUtils.anonymousUsersEnabled() && this.insight.getUser().isAnonymous()) {
				throwAnonymousUserError();
			}
			
			if(!SecurityProjectUtils.userCanEditProject(this.insight.getUser(), projectId)) {
				throw new IllegalArgumentException("User does not have permission to add insights in the project");
			}
			// Get the user's email
			AccessToken accessToken = user.getAccessToken(user.getPrimaryLogin());
			email = accessToken.getEmail();
			author = accessToken.getUsername();
		}
		
		String insightName = getInsightName();
		if(insightName == null || (insightName = insightName.trim()).isEmpty()) {
			throw new IllegalArgumentException("Need to define the insight name");
		}
		
		if(SecurityInsightUtils.insightNameExists(projectId, insightName) != null) {
			throw new IllegalArgumentException("Insight name already exists");
		}
		
		PixelList insightPixelList = null;
		List<String> recipeToSave = getRecipe();
		List<String> recipeIds = null;
		List<String> additionalSteps = null;
		List<ParamStruct> params = null;
		
		String layout = getLayout();
		boolean hidden = getHidden();
		Boolean cacheable = getUserDefinedCacheable();
		if(cacheable == null) {
			cacheable = Utility.getApplicationCacheInsight();
		}

		// saving an empty recipe?
		if (recipeToSave == null || recipeToSave.isEmpty()) {
			savingThisInsight = true;
			if(optimizeRecipe) {
				// optimize the recipe
				insightPixelList = PixelUtility.getOptimizedPixelList(this.insight);
			} else {
				// remove unnecessary pixels to start
				insightPixelList = this.insight.getPixelList();
			}
			
			recipeToSave = insightPixelList.getPixelRecipe();
			recipeIds = insightPixelList.getPixelIds();
			
			// now add the additional pixel steps on save
			int counter = 0;
			// make sure we pass the correct insight pixel list
			// for the optimized pixel or the regular one
			additionalSteps = PixelUtility.getMetaInsightRecipeSteps(this.insight, insightPixelList);
			for(String step : additionalSteps) {
				recipeToSave.add(step);
				// in case we are saving a not run recipe like forms
				if(recipeIds != null) {
					recipeIds.add(counter++ + "_additionalStep");
				}
			}
			params = InsightUtility.getInsightParams(this.insight);
		} else {
			// default for recipe encoded when no key is passed is true
			if(recipeEncoded()) {
				recipeToSave = decodeRecipe(recipeToSave);
			}
		}
		
		// get an updated recipe if there are files used
		// and save the files in the correct location
		// get the new insight id
		String newInsightId = UUID.randomUUID().toString();
		
		// pull the insights db again incase someone just saved something 
		IProject project = Utility.getProject(projectId);
		ClusterUtil.reactorPullInsightsDB(projectId);
		ClusterUtil.reactorPullProjectFolder(project, AssetUtility.getProjectAssetVersionFolder(project.getProjectName(), projectId));
		
		if(insightPixelList != null) {
			try {
				// if we are saving a saved insight as another insight
				// do not delete the file from this insight
				if(saveFilesInInsight(insightPixelList, projectId, newInsightId, !this.insight.isSavedInsight())) {
					// need to pull the new saved recipe
					recipeToSave = insightPixelList.getPixelRecipe();
				}
			} catch(Exception e) {
				throw new IllegalArgumentException("An error occured trying to identify file based sources to parameterize. The source error message is: " + e.getMessage(), e);
			}
		}
		
		// get an updated recipe if there are files used
		// and save the files in the correct location
		
		if(params != null && !params.isEmpty()) {
			try {
				recipeToSave = PixelUtility.parameterizeRecipe(this.insight, recipeToSave, recipeIds, params, insightName);
			} catch(Exception e) {
				throw new IllegalArgumentException("An error occured trying to parameterize the insight recipe. The source error message is: " + e.getMessage(), e);
			}
		}
		
		int stepCounter = 1;
		// add the recipe to the insights database
		InsightAdministrator admin = new InsightAdministrator(project.getInsightDatabase());
		logger.info(stepCounter + ") Add insight " + insightName + " to rdbms store...");
		String newRdbmsId = admin.addInsight(newInsightId, insightName, layout, recipeToSave, hidden, cacheable);
		logger.info(stepCounter +") Done...");
		stepCounter++;

		String description = getDescription();
		List<String> tags = getTags();
		
		if(!hidden) {
			logger.info(stepCounter + ") Regsiter insight...");
			registerInsightAndMetadata(project, newRdbmsId, insightName, layout, cacheable, recipeToSave, description, tags);
			logger.info(stepCounter + ") Done...");
		} else {
			logger.info(stepCounter + ") Insight is hidden ... do not add to solr");
		}
		stepCounter++;
		
		// Move assets to new insight folder
		File tempInsightFolder = new File(this.insight.getInsightFolder());
		File newInsightFolder = new File(AssetUtility.getProjectAssetVersionFolder(project.getProjectName(), projectId) + DIR_SEPARATOR + newRdbmsId);
		if(tempInsightFolder.exists()) {
			try {
				logger.info(stepCounter + ") Moving assets...");
				FileUtils.copyDirectory(tempInsightFolder, newInsightFolder);
				logger.info(stepCounter + ") Done...");
			} catch (IOException e) {
				SaveInsightReactor.logger.error(Constants.STACKTRACE, e);
				logger.info(stepCounter + ") Unable to move assets...");
			}
		} else {
			logger.info(stepCounter + ") No asset folder exists to move...");
		}
	    stepCounter++;
	    // delete the cache folder for the new insight
	 	InsightCacheUtility.deleteCache(project.getProjectId(), project.getProjectName(), newRdbmsId, false);

	 	// write recipe to file
	 	// force = true to delete any existing mosfet files that were pulled from asset folder
		logger.info(stepCounter + ") Add recipe to file...");
		try {
			MosfetSyncHelper.makeMosfitFile(project.getProjectId(), project.getProjectName(), 
					newRdbmsId, insightName, layout, recipeToSave, hidden, description, tags, true);
		} catch (IOException e) {
			SaveInsightReactor.logger.error(Constants.STACKTRACE, e);
			logger.info(stepCounter + ") Unable to save recipe file...");
		}
		logger.info(stepCounter + ") Done...");
		stepCounter++;
	 	
		// get file we are saving as an image
		String imageFile = getImage();
		if(imageFile != null && !imageFile.trim().isEmpty()) {
			logger.info(stepCounter + ") Storing insight image...");
			storeImageFromFile(imageFile, newRdbmsId, project.getProjectId(), project.getProjectName());
			logger.info(stepCounter + ") Done...");
			stepCounter++;
		}

		// adding insight files to git
		Stream<Path> walk = null;
		try {
			String folder = AssetUtility.getProjectAssetVersionFolder(project.getProjectName(), projectId);
			// grab relative file paths
			walk = Files.walk(Paths.get(newInsightFolder.toURI()));
			List<String> files = walk
					.map(x -> newInsightId + DIR_SEPARATOR
							+ newInsightFolder.toURI().relativize(new File(x.toString()).toURI()).getPath().toString())
					.collect(Collectors.toList());
			files.remove(""); // removing empty path
			logger.info(stepCounter + ") Adding insight to git...");
			GitRepoUtils.addSpecificFiles(folder, files);
			GitRepoUtils.commitAddedFiles(folder, GitUtils.getDateMessage("Saved "+ insightName +" insight on"), author, email);
			logger.info(stepCounter + ") Done...");
		} catch (Exception e) {
			SaveInsightReactor.logger.error(Constants.STACKTRACE, e);
			logger.info(stepCounter + ") Unable to add insight to git...");
		} finally {
			if(walk != null) {
				walk.close();
			}
		}
		stepCounter++;
		
		// update the workspace cache for the saved insight
		this.insight.setProjectId(projectId);
		this.insight.setProjectName(project.getProjectName());
		this.insight.setRdbmsId(newRdbmsId);
		this.insight.setInsightName(insightName);
		// this is to reset it
		this.insight.setInsightFolder(null);
		this.insight.setAppFolder(null);
		
		// add to the users opened insights
		if(savingThisInsight && this.insight.getUser() != null) {
			this.insight.getUser().addOpenInsight(projectId, newRdbmsId, this.insight.getInsightId());
		}
		
		ClusterUtil.reactorPushInsightDB(projectId);
		ClusterUtil.reactorPushProjectFolder(project, AssetUtility.getProjectAssetVersionFolder(project.getProjectName(), projectId));

		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("name", insightName);
		returnMap.put("app_insight_id", newRdbmsId);
		returnMap.put("app_name", project.getProjectName());
		returnMap.put("app_id", projectId);
		returnMap.put("recipe", recipeToSave);
		NounMetadata noun = new NounMetadata(returnMap, PixelDataType.CUSTOM_DATA_STRUCTURE, PixelOperationType.SAVE_INSIGHT);
		return noun;
	}
	
	/**
	 * Save a new insight within security database
	 * @param appId
	 * @param insightIdToSave
	 * @param insightName
	 * @param layout
	 * @param description
	 * @param tags
	 */
	private void registerInsightAndMetadata(IProject project, String insightIdToSave, String insightName, String layout, 
			boolean cacheable, List<String> recipe, String description, List<String> tags) {
		String projectId = project.getProjectId();
		// TODO: INSIGHTS ARE ALWAYS GLOBAL!!!
		SecurityInsightUtils.addInsight(projectId, insightIdToSave, insightName, true, cacheable, layout, recipe);
		if(this.insight.getUser() != null) {
			SecurityInsightUtils.addUserInsightCreator(this.insight.getUser(), projectId, insightIdToSave);
		}
		InsightAdministrator admin = new InsightAdministrator(project.getInsightDatabase());
		if(description != null) {
			admin.updateInsightDescription(insightIdToSave, description);
			SecurityInsightUtils.updateInsightDescription(projectId, insightIdToSave, description);
		}
		if(tags != null) {
			admin.updateInsightTags(insightIdToSave, tags);
			SecurityInsightUtils.updateInsightTags(projectId, insightIdToSave, tags);
		}
	}
}

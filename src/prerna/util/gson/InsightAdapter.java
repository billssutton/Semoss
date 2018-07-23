package prerna.util.gson;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import prerna.algorithm.api.ITableDataFrame;
import prerna.cache.CachePropFileFrameObject;
import prerna.om.Insight;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.VarStore;
import prerna.sablecc2.om.nounmeta.NounMetadata;

public class InsightAdapter extends TypeAdapter<Insight> {

	@Override
	public void write(JsonWriter out, Insight value) throws IOException {
		String engineId = value.getEngineId();
		String rdbmsId = value.getRdbmsId();
		
		if(engineId == null || rdbmsId == null) {
			throw new IOException("Cannot jsonify an insight that is not saved");
		}
		
		// start insight object
		out.beginObject();
		// write engine id
		out.name("engineId").value(engineId);
		// write rdbms id
		out.name("rdbmsId").value(rdbmsId);
		
		// write varstore
		out.name("varstore");
		// output all variables that are not frames or tasks
		VarStoreAdapter adapter = new VarStoreAdapter();
		VarStore store = value.getVarStore();
		adapter.write(out, store);
		
		List<FrameCacheHelper> frames = new Vector<FrameCacheHelper>();
		Set<String> keys = store.getKeys();
		for(String k : keys) {
			NounMetadata noun = store.get(k);
			PixelDataType type = noun.getNounType();
			if(type == PixelDataType.FRAME) {
				ITableDataFrame frame = (ITableDataFrame) noun.getValue();
				FrameCacheHelper existingFrameObject = findSameFrame(frames, frame);
				if(existingFrameObject != null) {
					existingFrameObject.addAlias(k);
				} else {
					FrameCacheHelper fObj = new FrameCacheHelper(frame);
					fObj.addAlias(k);
					frames.add(fObj);
				}
			}
		}
		
		String folderDir = "C:\\workspace\\testSave";
		
		// write the frames
		out.name("frames");
		out.beginArray();
		for(FrameCacheHelper fObj : frames) {
			CachePropFileFrameObject saveFrame = fObj.frame.save(folderDir);
			out.beginObject();
			out.name("file").value(saveFrame.getFrameFileLocation());
			out.name("meta").value(saveFrame.getFrameMetaLocation());
			out.name("type").value(saveFrame.getFrameType());
			out.name("name").value(saveFrame.getFrameName());
			out.name("keys");
			out.beginArray();
			for(int i = 0; i < fObj.alias.size(); i++) {
				out.value(fObj.alias.get(i));
			}
			out.endArray();
			out.endObject();
		}
		out.endArray();
		
		// end insight object
		out.endObject();
	}

	@Override
	public Insight read(JsonReader in) throws IOException {
		Insight insight = new Insight();
		
		in.beginObject();
		in.nextName();
		String engineId = in.nextString();
		insight.setEngineId(engineId);
		in.nextName();
		String rdbmsId = in.nextString();
		insight.setRdbmsId(rdbmsId);
		
		// this will be the varstore
		in.nextName();
		VarStoreAdapter adapter = new VarStoreAdapter();
		VarStore store = adapter.read(in);
		insight.setVarStore(store);
		
		// this will be the frames
		in.nextName();
		in.beginArray();
		while(in.hasNext()) {
			in.beginObject();
			
			// order is 
			// file
			// meta
			// type
			// name
			
			List<String> varStoreKeys = new Vector<String>();
			CachePropFileFrameObject cf = new CachePropFileFrameObject();
			while(in.hasNext()) {
				String k = in.nextName();
				if(k.equals("file")) {
					cf.setFrameFileLocation(in.nextString());
				} else if(k.equals("meta")) {
					cf.setFrameMetaLocation(in.nextString());
				} else if(k.equals("type")) {
					cf.setFrameType(in.nextString());
				} else if(k.equals("name")) {
					cf.setFrameName(in.nextString());
				} else if(k.equals("keys")) {
					in.beginArray();
					while(in.hasNext()) {
						varStoreKeys.add(in.nextString());
					}
					in.endArray();
				}
			}

			ITableDataFrame frame;
			try {
				frame = (ITableDataFrame) Class.forName(cf.getFrameType()).newInstance();
				frame.open(cf);
				
				NounMetadata fNoun = new NounMetadata(frame, PixelDataType.FRAME);
				for(String varStoreK : varStoreKeys) {
					store.put(varStoreK, fNoun);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
			in.endObject();
		}
		in.endArray();
		
		in.endObject();
		return insight;
	}

	private FrameCacheHelper findSameFrame(List<FrameCacheHelper> frames, ITableDataFrame frame) {
		int size = frames.size();
		for(int i = 0; i < size; i++) {
			if(frames.get(i).sameFrame(frame)) {
				return frames.get(i);
			}
		}
		
		return null;
	}
	
}

/**
 * Simple object to help cache frames
 */
class FrameCacheHelper {
	
	ITableDataFrame frame;
	List<String> alias = new Vector<String>();
	
	FrameCacheHelper(ITableDataFrame frame) {
		this.frame = frame;
	}
	
	public void addAlias(String alias) {
		this.alias.add(alias);
	}
	
	public boolean sameFrame(ITableDataFrame frame) {
		return this.frame == frame;
	}
}


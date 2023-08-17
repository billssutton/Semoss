package prerna.engine.impl.model.inferencetracking.reactors;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.engine.impl.model.inferencetracking.ModelInferenceLogsUtils;
import prerna.sablecc2.om.PixelDataType;
import prerna.sablecc2.om.nounmeta.NounMetadata;
import prerna.sablecc2.reactor.AbstractReactor;
import prerna.auth.User;

public class GetUserConversationRoomsReactor extends AbstractReactor {
	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(GetUserConversationRoomsReactor.class);

    public GetUserConversationRoomsReactor() {
        this.keysToGet = new String[] {};
    }
    
	@Override
	public NounMetadata execute() {
		organizeKeys();
		User user = this.insight.getUser();
        if (user == null) {
            throw new IllegalArgumentException("You are not properly logged in");
        }
        List<Map<String, Object>> output = ModelInferenceLogsUtils.getUserConversations(user.getPrimaryLoginToken().getId());
		return new NounMetadata(output, PixelDataType.VECTOR);
	}
}

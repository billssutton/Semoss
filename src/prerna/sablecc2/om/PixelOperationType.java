package prerna.sablecc2.om;

public enum PixelOperationType {

	// JOB INFORMATION
	TASK,
	RESET_PANEL_TASKS,
	REMOVE_TASK,
	REMOVE_LAYER,
	TASK_DATA,
	TASK_METADATA,
	
	// QUERY
	QUERY,
	QUERY_ROW_COUNT,
	
	// FRAME INFORMATION
	FRAME,
	FRAME_DATA_CHANGE,
	FRAME_HEADERS_CHANGE,
	REMOVE_FRAME,
	FRAME_CACHE,
	FRAME_SWAP,
	FRAME_MAP,
	FRAME_METAMODEL,
	FRAME_TABLE_STRUCTURE,
	// USED FOR ADDITIONAL OUTPUT
	ADD_HEADERS,
	REMOVE_HEADERS,
	MODIFY_HEADERS,
	
	FRAME_HEADERS,
	FRAME_FILTER,
	FRAME_FILTER_CHANGE,
	FILTER_MODEL,
	
	// SHEET OPERATIONS
	SHEET,
	SHEET_OPEN,
	SHEET_CLOSE,
	SHEET_LABEL,
	SHEET_BACKGROUND,
	SHEET_CONFIG,
	CACHED_SHEET,
	
	// PANEL OPERATIONS
	PANEL,
	PANEL_OPEN,
	PANEL_OPEN_IF_ABSENT,
	PANEL_CLOSE,
	PANEL_CONFIG,
	PANEL_ORNAMENT,
	PANEL_VIEW,
	PANEL_LABEL,
	PANEL_HEADER,
	PANEL_CLONE,
	PANEL_MOVE,
	PANEL_COMMENT,
	PANEL_EVENT,
	PANEL_FILTER,
	PANEL_FILTER_CHANGE,
	PANEL_FILTER_MODEL,
	PANEL_SORT,
	PANEL_COLLECT,
	CACHED_PANEL,
	CACHED_PANEL_CLONE,
	
	// this is only for legacy insights/dashboards
	@Deprecated
	PANEL_POSITION,
	
	// PANEL LAYERS
	PANEL_ORNAMENT_DATA,
	PANEL_COLOR_BY_VALUE,
	ADD_PANEL_COLOR_BY_VALUE,
	REMOVE_PANEL_COLOR_BY_VALUE,
	PANEL_COLOR_BY_VALUE_LIST,

	// EXTERNAL WINDOWS
	OPEN_TAB,
	
	// META DATA INFORMATION
	DATABASE_INFO,
	DATABASE_USERS,
	APP_INSIGHTS,
	USER_INFO,
	
	// PROJECT
	PROJECT_LIST,
	PROJECT_INFO,
	DELETE_PROJECT,
	
	// forms calls that change the db
	ALTER_DATABASE,
	
	// these are the new traverse options
	DATABASE_TABLE_STRUCTURE,
	DATABASE_TRAVERSE_OPTIONS,
	TRAVERSAL_OPTIONS,
	WIKI_LOGICAL_NAMES,
	CONNECTED_CONCEPTS,
	DATABASE_LIST,
	DATABASE_METAMODEL,
	DATABASE_DICTIONARY,
	DATABASE_CONCEPTS,
	DATABASE_CONCEPT_PROPERTIES,
	DATABASE_PIXEL_SELECTORS,
	ENTITY_LOGICAL_NAMES,
	ENTITY_DESCRIPTIONS,
	
	// APP SPECIFIC WIDGETS
	APP_WIDGETS, 
	
	//Database
	DELETE_ENGINE,
	
	// INSIGHT INFORMATION
	CURRENT_INSIGHT_RECIPE,
	SAVED_INSIGHT_RECIPE,
	RERUN_INSIGHT_RECIPE,
	PARAMETER_EXECUTION,
	OPEN_SAVED_INSIGHT,
	LOAD_INSIGHT,
	NEW_EMPTY_INSIGHT,
	DROP_INSIGHT,
	CLEAR_INSIGHT,
	INSIGHT_HANDLE,
	SAVE_INSIGHT,
	INSIGHT_ORNAMENT,
	INSIGHT_THEME,
	DELETE_INSIGHT,
	PIPELINE,
	
	// INSIGHT CONFIG
	INSIGHT_CONFIG,
	GOLDEN_LAYOUT,
	
	// DASHBAORD
	DASHBOARD_INSIGHT_CONFIGURATION,
	
	// RUNNING JAVA CODE
	CODE_EXECUTION,
	// MULTI OUTPUT
	VECTOR,
	
	// REMOVE VARIABLE
	REMOVE_VARIABLE,
	
	// A SUBSCRIPT WITHIN YOUR SCRIPT
	// SO THE FE KNOWS TO LOOP THROUGH
	// AN ARRAY OF RESULTS
	SUB_SCRIPT,
	
	// ROUTINES THAT SEND BACK DATA TO VISUALIZE WITH A LAYOUT
	VIZ_OUTPUT,
	
	// FILE DOWNLOAD
	FILE_DOWNLOAD,
	FILE,
	
	// OLD INSIGHT
	OLD_INSIGHT,
	PLAYSHEET_PARAMS,
	
	// GIT_MARKET
	MARKET_PLACE, // general market routine
	MARKET_PLACE_INIT,
	MARKET_PLACE_ADDITION,
	
	// SCHEDULER INFORMATION
	SCHEDULE_JOB,
	LIST_JOB, 
	RESCHEDULE_JOB, 
	UNSCHEDULE_JOB, 
	
	// USER ANALYTICS
	VIZ_RECOMMENDATION,
	RECOMMENDATION,

	// CLOUD
	GOOGLE_SHEET_LIST,
	GOOGLE_DRIVE_LIST,
	CLOUD_FILE_LIST,
	S3,
	
	// Cluster
	OPEN_DATABASE,
	USER_UPLOAD,
	UPDATE_APP,
	CLEANUP_APPS,
	VERSION,
	SYNC_APPS,
	
	// User Space
	USER_DIR,
	
	// RECIPE COMMENTS
	RECIPE_COMMENT,
	
	// R/PY PACKAGES
	CHECK_R_PACKAGES,
	CHECK_PY_PACKAGES,

	// SOME KIND OF OPERATION THAT WE WANT TO OUTPUT
	OPERATION,
	
	// FORCE SAVE THE RECIPE STEP
	FORCE_SAVE_DATA_TRANSFORMATION,
	FORCE_SAVE_VISUALIZATION,
	FORCE_SAVE_DATA_EXPORT,
	
	// HELP
	HELP,
	
	// JOB REACTOR
	JOB_ID,
	
	// MESSAGES ERRORS
	SUCCESS,
	WARNING,
	ERROR,
	UNEXECUTED_PIXELS,
	FRAME_SIZE_LIMIT_EXCEEDED,
	USER_INPUT_REQUIRED,
	LOGGIN_REQUIRED_ERROR,
	ANONYMOUS_USER_ERROR,
	INVALID_SYNTAX;
}

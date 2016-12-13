/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.analysis;

import prerna.sablecc.node.*;

public interface Analysis extends Switch
{
    Object getIn(Node node);
    void setIn(Node node, Object o);
    Object getOut(Node node);
    void setOut(Node node, Object o);

    void caseStart(Start node);
    void caseAConfiguration(AConfiguration node);
    void caseAColopScript(AColopScript node);
    void caseAVaropScript(AVaropScript node);
    void caseAJOpScript(AJOpScript node);
    void caseAExprScript(AExprScript node);
    void caseAHelpScript(AHelpScript node);
    void caseAPanelopScript(APanelopScript node);
    void caseAScript(AScript node);
    void caseADataopScript(ADataopScript node);
    void caseADashboardopScript(ADashboardopScript node);
    void caseADatabaseopScript(ADatabaseopScript node);
    void caseAAddColumnColop(AAddColumnColop node);
    void caseARemcolColop(ARemcolColop node);
    void caseASetcolColop(ASetcolColop node);
    void caseAPivotcolColop(APivotcolColop node);
    void caseAFiltercolColop(AFiltercolColop node);
    void caseAFiltermodelColop(AFiltermodelColop node);
    void caseAFocuscolColop(AFocuscolColop node);
    void caseAUnfocusColop(AUnfocusColop node);
    void caseAImportColop(AImportColop node);
    void caseAAliasColop(AAliasColop node);
    void caseAImportDataColop(AImportDataColop node);
    void caseAUnfiltercolColop(AUnfiltercolColop node);
    void caseARemoveDataColop(ARemoveDataColop node);
    void caseADataFrameColop(ADataFrameColop node);
    void caseADataFrameHeaderColop(ADataFrameHeaderColop node);
    void caseADataFrameDuplicatesColop(ADataFrameDuplicatesColop node);
    void caseAOpenDataColop(AOpenDataColop node);
    void caseARenamecolColop(ARenamecolColop node);
    void caseASplitcolColop(ASplitcolColop node);
    void caseADashboardJoinColop(ADashboardJoinColop node);
    void caseAQueryDataColop(AQueryDataColop node);
    void caseAClearCacheColop(AClearCacheColop node);
    void caseAUseCacheColop(AUseCacheColop node);
    void caseAPanelVizPanelop(APanelVizPanelop node);
    void caseAPanelCommentPanelop(APanelCommentPanelop node);
    void caseAPanelCommentRemovePanelop(APanelCommentRemovePanelop node);
    void caseAPanelCommentEditPanelop(APanelCommentEditPanelop node);
    void caseAPanelLookAndFeelPanelop(APanelLookAndFeelPanelop node);
    void caseAPanelToolsPanelop(APanelToolsPanelop node);
    void caseAPanelConfigPanelop(APanelConfigPanelop node);
    void caseAPanelClonePanelop(APanelClonePanelop node);
    void caseAPanelClosePanelop(APanelClosePanelop node);
    void caseAOutputInsightPanelop(AOutputInsightPanelop node);
    void caseAPanelHandlePanelop(APanelHandlePanelop node);
    void caseADatatypeDataop(ADatatypeDataop node);
    void caseADataconnectDataop(ADataconnectDataop node);
    void caseADataconnectdbDataop(ADataconnectdbDataop node);
    void caseADatanetworkconnectDataop(ADatanetworkconnectDataop node);
    void caseADatanetworkdisconnectDataop(ADatanetworkdisconnectDataop node);
    void caseAOutputDataDataop(AOutputDataDataop node);
    void caseADataModelDataop(ADataModelDataop node);
    void caseAClearDataDataop(AClearDataDataop node);
    void caseADashboardConfigDashboardop(ADashboardConfigDashboardop node);
    void caseADashboardAddDashboardop(ADashboardAddDashboardop node);
    void caseADatabaseListDatabaseop(ADatabaseListDatabaseop node);
    void caseADatabaseConceptsDatabaseop(ADatabaseConceptsDatabaseop node);
    void caseADatabaseConnectedConceptsDatabaseop(ADatabaseConnectedConceptsDatabaseop node);
    void caseADatabaseMetamodelDatabaseop(ADatabaseMetamodelDatabaseop node);
    void caseADatabaseConceptPropertiesDatabaseop(ADatabaseConceptPropertiesDatabaseop node);
    void caseAPanelViz(APanelViz node);
    void caseAPanelComment(APanelComment node);
    void caseAPanelCommentEdit(APanelCommentEdit node);
    void caseAPanelCommentRemove(APanelCommentRemove node);
    void caseAPanelLookAndFeel(APanelLookAndFeel node);
    void caseAPanelTools(APanelTools node);
    void caseAPanelConfig(APanelConfig node);
    void caseAPanelClone(APanelClone node);
    void caseAPanelClose(APanelClose node);
    void caseAPanelHandle(APanelHandle node);
    void caseADataFrame(ADataFrame node);
    void caseADataFrameHeader(ADataFrameHeader node);
    void caseADataFrameDuplicates(ADataFrameDuplicates node);
    void caseADashboardConfig(ADashboardConfig node);
    void caseAAddColumn(AAddColumn node);
    void caseARemColumn(ARemColumn node);
    void caseASetColumn(ASetColumn node);
    void caseAPivotColumn(APivotColumn node);
    void caseAFilterColumn(AFilterColumn node);
    void caseAFilterModel(AFilterModel node);
    void caseAUnfilterColumn(AUnfilterColumn node);
    void caseAFocusColumn(AFocusColumn node);
    void caseAUnfocus(AUnfocus node);
    void caseAImportColumn(AImportColumn node);
    void caseAAliasColumn(AAliasColumn node);
    void caseARenameColumn(ARenameColumn node);
    void caseASplitColumn(ASplitColumn node);
    void caseAImportData(AImportData node);
    void caseAQueryData(AQueryData node);
    void caseAOpenData(AOpenData node);
    void caseAClearCache(AClearCache node);
    void caseAUseCache(AUseCache node);
    void caseAOutputData(AOutputData node);
    void caseAClearData(AClearData node);
    void caseAApiImportBlock(AApiImportBlock node);
    void caseACsvTableImportBlock(ACsvTableImportBlock node);
    void caseAPastedDataImportBlock(APastedDataImportBlock node);
    void caseARawApiImportBlock(ARawApiImportBlock node);
    void caseAPastedDataBlock(APastedDataBlock node);
    void caseAPastedData(APastedData node);
    void caseARemoveData(ARemoveData node);
    void caseADecimal(ADecimal node);
    void caseAExprGroup(AExprGroup node);
    void caseAOutputInsight(AOutputInsight node);
    void caseAApiBlock(AApiBlock node);
    void caseARawApiBlock(ARawApiBlock node);
    void caseASelector(ASelector node);
    void caseAColWhere(AColWhere node);
    void caseAColDefColDefOrCsvRow(AColDefColDefOrCsvRow node);
    void caseACsvColDefOrCsvRow(ACsvColDefOrCsvRow node);
    void caseAColWhereGroup(AColWhereGroup node);
    void caseAWhereClause(AWhereClause node);
    void caseAWhereStatement(AWhereStatement node);
    void caseARelationDef(ARelationDef node);
    void caseARelationGroup(ARelationGroup node);
    void caseARelationClause(ARelationClause node);
    void caseAIfBlock(AIfBlock node);
    void caseAColGroup(AColGroup node);
    void caseAKeyvalue(AKeyvalue node);
    void caseAKeyvalueGroup(AKeyvalueGroup node);
    void caseAMapObj(AMapObj node);
    void caseAGroupBy(AGroupBy node);
    void caseAColDef(AColDef node);
    void caseATableDef(ATableDef node);
    void caseAVarDef(AVarDef node);
    void caseAVarop(AVarop node);
    void caseACsvRow(ACsvRow node);
    void caseAEasyRow(AEasyRow node);
    void caseAEasyGroup(AEasyGroup node);
    void caseACsvTable(ACsvTable node);
    void caseAColCsv(AColCsv node);
    void caseAColTable(AColTable node);
    void caseANumWordOrNum(ANumWordOrNum node);
    void caseAAlphaWordOrNum(AAlphaWordOrNum node);
    void caseAExprWordOrNum(AExprWordOrNum node);
    void caseAWordOrNumWordOrNumOrNestedObj(AWordOrNumWordOrNumOrNestedObj node);
    void caseANestedMapWordOrNumOrNestedObj(ANestedMapWordOrNumOrNestedObj node);
    void caseANestedCsvWordOrNumOrNestedObj(ANestedCsvWordOrNumOrNestedObj node);
    void caseAFlexSelectorRow(AFlexSelectorRow node);
    void caseASelectorTerm(ASelectorTerm node);
    void caseASelectorGroup(ASelectorGroup node);
    void caseAFormula(AFormula node);
    void caseACsvGroup(ACsvGroup node);
    void caseAExprRow(AExprRow node);
    void caseADashboardJoin(ADashboardJoin node);
    void caseADashboardAdd(ADashboardAdd node);
    void caseAOpenDataJoinParam(AOpenDataJoinParam node);
    void caseAInsightidJoinParam(AInsightidJoinParam node);
    void caseAVariableJoinParam(AVariableJoinParam node);
    void caseAJoinGroup(AJoinGroup node);
    void caseAJoinParamList(AJoinParamList node);
    void caseAJOp(AJOp node);
    void caseAHelp(AHelp node);
    void caseADatatype(ADatatype node);
    void caseADataconnect(ADataconnect node);
    void caseADatanetworkconnect(ADatanetworkconnect node);
    void caseADatanetworkdisconnect(ADatanetworkdisconnect node);
    void caseADataconnectdb(ADataconnectdb node);
    void caseADataModel(ADataModel node);
    void caseAComparatorEqualOrCompare(AComparatorEqualOrCompare node);
    void caseAEqualEqualOrCompare(AEqualEqualOrCompare node);
    void caseAUserInput(AUserInput node);
    void caseAExprInputOrExpr(AExprInputOrExpr node);
    void caseAInputInputOrExpr(AInputInputOrExpr node);
    void caseAOpenDataInputOrExpr(AOpenDataInputOrExpr node);
    void caseACondition(ACondition node);
    void caseAConditionGroup(AConditionGroup node);
    void caseAConditionBlock(AConditionBlock node);
    void caseATermExpr(ATermExpr node);
    void caseAPlusExpr(APlusExpr node);
    void caseAMinusExpr(AMinusExpr node);
    void caseAMultExpr(AMultExpr node);
    void caseADivExpr(ADivExpr node);
    void caseAModExpr(AModExpr node);
    void caseAEExprExpr(AEExprExpr node);
    void caseAConditionExprExpr(AConditionExprExpr node);
    void caseAMathFun(AMathFun node);
    void caseAOptionsMap(AOptionsMap node);
    void caseAExtendedExpr(AExtendedExpr node);
    void caseANumberTerm(ANumberTerm node);
    void caseAFormulaTerm(AFormulaTerm node);
    void caseAVarTerm(AVarTerm node);
    void caseAColTerm(AColTerm node);
    void caseAApiTerm(AApiTerm node);
    void caseATabTerm(ATabTerm node);
    void caseACsvTerm(ACsvTerm node);
    void caseATerm(ATerm node);
    void caseAAlphaTerm(AAlphaTerm node);
    void caseAMathFunTerm(AMathFunTerm node);
    void caseACodeblockTerm(ACodeblockTerm node);
    void caseADatabaseList(ADatabaseList node);
    void caseADatabaseConcepts(ADatabaseConcepts node);
    void caseADatabaseConnectedConcepts(ADatabaseConnectedConcepts node);
    void caseADatabaseConceptProperties(ADatabaseConceptProperties node);
    void caseADatabaseMetamodel(ADatabaseMetamodel node);

    void caseTNumber(TNumber node);
    void caseTBoolean(TBoolean node);
    void caseTId(TId node);
    void caseTDot(TDot node);
    void caseTSemicolon(TSemicolon node);
    void caseTColon(TColon node);
    void caseTPlus(TPlus node);
    void caseTMinus(TMinus node);
    void caseTMult(TMult node);
    void caseTComma(TComma node);
    void caseTDiv(TDiv node);
    void caseTCol(TCol node);
    void caseTComparator(TComparator node);
    void caseTVizType(TVizType node);
    void caseTLogOperator(TLogOperator node);
    void caseTEqual(TEqual node);
    void caseTColadd(TColadd node);
    void caseTApi(TApi node);
    void caseTMath(TMath node);
    void caseTColjoin(TColjoin node);
    void caseTColprefix(TColprefix node);
    void caseTTablePrefix(TTablePrefix node);
    void caseTValprefix(TValprefix node);
    void caseTColremove(TColremove node);
    void caseTColfilter(TColfilter node);
    void caseTColunfilter(TColunfilter node);
    void caseTColfiltermodel(TColfiltermodel node);
    void caseTColimport(TColimport node);
    void caseTColset(TColset node);
    void caseTColpivot(TColpivot node);
    void caseTColfocus(TColfocus node);
    void caseTColalias(TColalias node);
    void caseTColrename(TColrename node);
    void caseTColsplit(TColsplit node);
    void caseTCollink(TCollink node);
    void caseTShowHide(TShowHide node);
    void caseTMod(TMod node);
    void caseTLPar(TLPar node);
    void caseTRPar(TRPar node);
    void caseTLBracket(TLBracket node);
    void caseTRBracket(TRBracket node);
    void caseTLCurlBracket(TLCurlBracket node);
    void caseTRCurlBracket(TRCurlBracket node);
    void caseTGroup(TGroup node);
    void caseTBlank(TBlank node);
    void caseTSpace(TSpace node);
    void caseTNewline(TNewline node);
    void caseTJava(TJava node);
    void caseTPython(TPython node);
    void caseTProc(TProc node);
    void caseTThis(TThis node);
    void caseTNull(TNull node);
    void caseTImportType(TImportType node);
    void caseTRelType(TRelType node);
    void caseTDataimporttoken(TDataimporttoken node);
    void caseTDataremovetoken(TDataremovetoken node);
    void caseTDataopentoken(TDataopentoken node);
    void caseTDataoutputtoken(TDataoutputtoken node);
    void caseTDatacleartoken(TDatacleartoken node);
    void caseTDataquerytoken(TDataquerytoken node);
    void caseTDatamodeltoken(TDatamodeltoken node);
    void caseTDataclearcachetoken(TDataclearcachetoken node);
    void caseTDatausecachetoken(TDatausecachetoken node);
    void caseTLiteral(TLiteral node);
    void caseTHelpToken(THelpToken node);
    void caseTCodeblock(TCodeblock node);
    void caseTJsonblock(TJsonblock node);
    void caseTQueryblock(TQueryblock node);
    void caseTWord(TWord node);
    void caseTPanelviz(TPanelviz node);
    void caseTPanelclone(TPanelclone node);
    void caseTPanelclose(TPanelclose node);
    void caseTPanelhandle(TPanelhandle node);
    void caseTDataframe(TDataframe node);
    void caseTDataframeheader(TDataframeheader node);
    void caseTDataframeduplicates(TDataframeduplicates node);
    void caseTFileText(TFileText node);
    void caseTPanelcommentremove(TPanelcommentremove node);
    void caseTPanelcommentedit(TPanelcommentedit node);
    void caseTPanelcommentadd(TPanelcommentadd node);
    void caseTPanellookandfeel(TPanellookandfeel node);
    void caseTPaneltools(TPaneltools node);
    void caseTPanelconfig(TPanelconfig node);
    void caseTOutputToken(TOutputToken node);
    void caseTUserinput(TUserinput node);
    void caseTJoin(TJoin node);
    void caseTDatatypeToken(TDatatypeToken node);
    void caseTDataconnectToken(TDataconnectToken node);
    void caseTDatanetworkconnectToken(TDatanetworkconnectToken node);
    void caseTDatanetworkdisconnectToken(TDatanetworkdisconnectToken node);
    void caseTDataconnectdbToken(TDataconnectdbToken node);
    void caseTDatabaselistToken(TDatabaselistToken node);
    void caseTDatabaseconceptsToken(TDatabaseconceptsToken node);
    void caseTDatabaseconnectedconceptsToken(TDatabaseconnectedconceptsToken node);
    void caseTDatabaseconceptpropertiesToken(TDatabaseconceptpropertiesToken node);
    void caseTDatabasemetamodelToken(TDatabasemetamodelToken node);
    void caseTDashboardconfig(TDashboardconfig node);
    void caseTDashboardAddToken(TDashboardAddToken node);
    void caseTWherestr(TWherestr node);
    void caseEOF(EOF node);
    void caseInvalidToken(InvalidToken node);
}

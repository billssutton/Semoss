/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.analysis;

import java.util.*;
import prerna.sablecc.node.*;

public class AnalysisAdapter implements Analysis
{
    private Hashtable<Node,Object> in;
    private Hashtable<Node,Object> out;

    public Object getIn(Node node)
    {
        if(this.in == null)
        {
            return null;
        }

        return this.in.get(node);
    }

    public void setIn(Node node, Object o)
    {
        if(this.in == null)
        {
            this.in = new Hashtable<Node,Object>(1);
        }

        if(o != null)
        {
            this.in.put(node, o);
        }
        else
        {
            this.in.remove(node);
        }
    }

    public Object getOut(Node node)
    {
        if(this.out == null)
        {
            return null;
        }

        return this.out.get(node);
    }

    public void setOut(Node node, Object o)
    {
        if(this.out == null)
        {
            this.out = new Hashtable<Node,Object>(1);
        }

        if(o != null)
        {
            this.out.put(node, o);
        }
        else
        {
            this.out.remove(node);
        }
    }

    public void caseStart(Start node)
    {
        defaultCase(node);
    }

    public void caseAConfiguration(AConfiguration node)
    {
        defaultCase(node);
    }

    public void caseAColopScript(AColopScript node)
    {
        defaultCase(node);
    }

    public void caseAVaropScript(AVaropScript node)
    {
        defaultCase(node);
    }

    public void caseAROpScript(AROpScript node)
    {
        defaultCase(node);
    }

    public void caseAExprScript(AExprScript node)
    {
        defaultCase(node);
    }

    public void caseAHelpScript(AHelpScript node)
    {
        defaultCase(node);
    }

    public void caseAPanelopScript(APanelopScript node)
    {
        defaultCase(node);
    }

    public void caseAScript(AScript node)
    {
        defaultCase(node);
    }

    public void caseAAddColumnColop(AAddColumnColop node)
    {
        defaultCase(node);
    }

    public void caseARemcolColop(ARemcolColop node)
    {
        defaultCase(node);
    }

    public void caseASetcolColop(ASetcolColop node)
    {
        defaultCase(node);
    }

    public void caseAPivotcolColop(APivotcolColop node)
    {
        defaultCase(node);
    }

    public void caseAFiltercolColop(AFiltercolColop node)
    {
        defaultCase(node);
    }

    public void caseAFocuscolColop(AFocuscolColop node)
    {
        defaultCase(node);
    }

    public void caseAUnfocusColop(AUnfocusColop node)
    {
        defaultCase(node);
    }

    public void caseAImportColop(AImportColop node)
    {
        defaultCase(node);
    }

    public void caseAAliasColop(AAliasColop node)
    {
        defaultCase(node);
    }

    public void caseAImportDataColop(AImportDataColop node)
    {
        defaultCase(node);
    }

    public void caseAUnfiltercolColop(AUnfiltercolColop node)
    {
        defaultCase(node);
    }

    public void caseARemoveDataColop(ARemoveDataColop node)
    {
        defaultCase(node);
    }

    public void caseADataFrameColop(ADataFrameColop node)
    {
        defaultCase(node);
    }

    public void caseAPanelVizPanelop(APanelVizPanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelCommentPanelop(APanelCommentPanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelCommentRemovePanelop(APanelCommentRemovePanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelCommentEditPanelop(APanelCommentEditPanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelClonePanelop(APanelClonePanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelClosePanelop(APanelClosePanelop node)
    {
        defaultCase(node);
    }

    public void caseAPanelViz(APanelViz node)
    {
        defaultCase(node);
    }

    public void caseAPanelComment(APanelComment node)
    {
        defaultCase(node);
    }

    public void caseAPanelCommentEdit(APanelCommentEdit node)
    {
        defaultCase(node);
    }

    public void caseAPanelCommentRemove(APanelCommentRemove node)
    {
        defaultCase(node);
    }

    public void caseAPanelClone(APanelClone node)
    {
        defaultCase(node);
    }

    public void caseAPanelClose(APanelClose node)
    {
        defaultCase(node);
    }

    public void caseADataFrame(ADataFrame node)
    {
        defaultCase(node);
    }

    public void caseAAddColumn(AAddColumn node)
    {
        defaultCase(node);
    }

    public void caseARemColumn(ARemColumn node)
    {
        defaultCase(node);
    }

    public void caseASetColumn(ASetColumn node)
    {
        defaultCase(node);
    }

    public void caseAPivotColumn(APivotColumn node)
    {
        defaultCase(node);
    }

    public void caseAFilterColumn(AFilterColumn node)
    {
        defaultCase(node);
    }

    public void caseAUnfilterColumn(AUnfilterColumn node)
    {
        defaultCase(node);
    }

    public void caseAFocusColumn(AFocusColumn node)
    {
        defaultCase(node);
    }

    public void caseAUnfocus(AUnfocus node)
    {
        defaultCase(node);
    }

    public void caseAImportColumn(AImportColumn node)
    {
        defaultCase(node);
    }

    public void caseAAliasColumn(AAliasColumn node)
    {
        defaultCase(node);
    }

    public void caseAImportData(AImportData node)
    {
        defaultCase(node);
    }

    public void caseAApiImportBlock(AApiImportBlock node)
    {
        defaultCase(node);
    }

    public void caseACsvTableImportBlock(ACsvTableImportBlock node)
    {
        defaultCase(node);
    }

    public void caseAPastedDataImportBlock(APastedDataImportBlock node)
    {
        defaultCase(node);
    }

    public void caseAPastedDataBlock(APastedDataBlock node)
    {
        defaultCase(node);
    }

    public void caseAPastedData(APastedData node)
    {
        defaultCase(node);
    }

    public void caseARemoveData(ARemoveData node)
    {
        defaultCase(node);
    }

    public void caseADecimal(ADecimal node)
    {
        defaultCase(node);
    }

    public void caseAExprGroup(AExprGroup node)
    {
        defaultCase(node);
    }

    public void caseAApiBlock(AApiBlock node)
    {
        defaultCase(node);
    }

    public void caseASelector(ASelector node)
    {
        defaultCase(node);
    }

    public void caseAColWhere(AColWhere node)
    {
        defaultCase(node);
    }

    public void caseAColDefColDefOrCsvRow(AColDefColDefOrCsvRow node)
    {
        defaultCase(node);
    }

    public void caseACsvColDefOrCsvRow(ACsvColDefOrCsvRow node)
    {
        defaultCase(node);
    }

    public void caseAColWhereGroup(AColWhereGroup node)
    {
        defaultCase(node);
    }

    public void caseAWhereClause(AWhereClause node)
    {
        defaultCase(node);
    }

    public void caseAWhereStatement(AWhereStatement node)
    {
        defaultCase(node);
    }

    public void caseARelationDef(ARelationDef node)
    {
        defaultCase(node);
    }

    public void caseARelationGroup(ARelationGroup node)
    {
        defaultCase(node);
    }

    public void caseARelationClause(ARelationClause node)
    {
        defaultCase(node);
    }

    public void caseAIfBlock(AIfBlock node)
    {
        defaultCase(node);
    }

    public void caseAColGroup(AColGroup node)
    {
        defaultCase(node);
    }

    public void caseAKeyvalue(AKeyvalue node)
    {
        defaultCase(node);
    }

    public void caseAKeyvalueGroup(AKeyvalueGroup node)
    {
        defaultCase(node);
    }

    public void caseAMapObj(AMapObj node)
    {
        defaultCase(node);
    }

    public void caseAGroupBy(AGroupBy node)
    {
        defaultCase(node);
    }

    public void caseAColDef(AColDef node)
    {
        defaultCase(node);
    }

    public void caseATableDef(ATableDef node)
    {
        defaultCase(node);
    }

    public void caseAVarDef(AVarDef node)
    {
        defaultCase(node);
    }

    public void caseAVarop(AVarop node)
    {
        defaultCase(node);
    }

    public void caseACsvRow(ACsvRow node)
    {
        defaultCase(node);
    }

    public void caseAEasyRow(AEasyRow node)
    {
        defaultCase(node);
    }

    public void caseAEasyGroup(AEasyGroup node)
    {
        defaultCase(node);
    }

    public void caseACsvTable(ACsvTable node)
    {
        defaultCase(node);
    }

    public void caseAColCsv(AColCsv node)
    {
        defaultCase(node);
    }

    public void caseANumWordOrNum(ANumWordOrNum node)
    {
        defaultCase(node);
    }

    public void caseAAlphaWordOrNum(AAlphaWordOrNum node)
    {
        defaultCase(node);
    }

    public void caseAExprWordOrNum(AExprWordOrNum node)
    {
        defaultCase(node);
    }

    public void caseAWordOrNumWordOrNumOrNestedMap(AWordOrNumWordOrNumOrNestedMap node)
    {
        defaultCase(node);
    }

    public void caseANestedMapWordOrNumOrNestedMap(ANestedMapWordOrNumOrNestedMap node)
    {
        defaultCase(node);
    }

    public void caseAFlexSelectorRow(AFlexSelectorRow node)
    {
        defaultCase(node);
    }

    public void caseATermGroup(ATermGroup node)
    {
        defaultCase(node);
    }

    public void caseAFormula(AFormula node)
    {
        defaultCase(node);
    }

    public void caseACsvGroup(ACsvGroup node)
    {
        defaultCase(node);
    }

    public void caseAExprRow(AExprRow node)
    {
        defaultCase(node);
    }

    public void caseAJOp(AJOp node)
    {
        defaultCase(node);
    }

    public void caseAROp(AROp node)
    {
        defaultCase(node);
    }

    public void caseAHelp(AHelp node)
    {
        defaultCase(node);
    }

    public void caseATermExpr(ATermExpr node)
    {
        defaultCase(node);
    }

    public void caseAPlusExpr(APlusExpr node)
    {
        defaultCase(node);
    }

    public void caseAMinusExpr(AMinusExpr node)
    {
        defaultCase(node);
    }

    public void caseAMultExpr(AMultExpr node)
    {
        defaultCase(node);
    }

    public void caseAExpr(AExpr node)
    {
        defaultCase(node);
    }

    public void caseADivExpr(ADivExpr node)
    {
        defaultCase(node);
    }

    public void caseAModExpr(AModExpr node)
    {
        defaultCase(node);
    }

    public void caseAEExprExpr(AEExprExpr node)
    {
        defaultCase(node);
    }

    public void caseAMathFun(AMathFun node)
    {
        defaultCase(node);
    }

    public void caseAExtendedExpr(AExtendedExpr node)
    {
        defaultCase(node);
    }

    public void caseANumberTerm(ANumberTerm node)
    {
        defaultCase(node);
    }

    public void caseAExprTerm(AExprTerm node)
    {
        defaultCase(node);
    }

    public void caseAVarTerm(AVarTerm node)
    {
        defaultCase(node);
    }

    public void caseAColTerm(AColTerm node)
    {
        defaultCase(node);
    }

    public void caseAApiTerm(AApiTerm node)
    {
        defaultCase(node);
    }

    public void caseATabTerm(ATabTerm node)
    {
        defaultCase(node);
    }

    public void caseAWcsvTerm(AWcsvTerm node)
    {
        defaultCase(node);
    }

    public void caseATerm(ATerm node)
    {
        defaultCase(node);
    }

    public void caseAAlphaTerm(AAlphaTerm node)
    {
        defaultCase(node);
    }

    public void caseAMathFunTerm(AMathFunTerm node)
    {
        defaultCase(node);
    }

    public void caseTNumber(TNumber node)
    {
        defaultCase(node);
    }

    public void caseTId(TId node)
    {
        defaultCase(node);
    }

    public void caseTDot(TDot node)
    {
        defaultCase(node);
    }

    public void caseTSemicolon(TSemicolon node)
    {
        defaultCase(node);
    }

    public void caseTColon(TColon node)
    {
        defaultCase(node);
    }

    public void caseTPlus(TPlus node)
    {
        defaultCase(node);
    }

    public void caseTMinus(TMinus node)
    {
        defaultCase(node);
    }

    public void caseTMult(TMult node)
    {
        defaultCase(node);
    }

    public void caseTComma(TComma node)
    {
        defaultCase(node);
    }

    public void caseTDiv(TDiv node)
    {
        defaultCase(node);
    }

    public void caseTCol(TCol node)
    {
        defaultCase(node);
    }

    public void caseTComparator(TComparator node)
    {
        defaultCase(node);
    }

    public void caseTColadd(TColadd node)
    {
        defaultCase(node);
    }

    public void caseTApi(TApi node)
    {
        defaultCase(node);
    }

    public void caseTMath(TMath node)
    {
        defaultCase(node);
    }

    public void caseTColjoin(TColjoin node)
    {
        defaultCase(node);
    }

    public void caseTColprefix(TColprefix node)
    {
        defaultCase(node);
    }

    public void caseTTablePrefix(TTablePrefix node)
    {
        defaultCase(node);
    }

    public void caseTValprefix(TValprefix node)
    {
        defaultCase(node);
    }

    public void caseTColremove(TColremove node)
    {
        defaultCase(node);
    }

    public void caseTColfilter(TColfilter node)
    {
        defaultCase(node);
    }

    public void caseTColunfilter(TColunfilter node)
    {
        defaultCase(node);
    }

    public void caseTColimport(TColimport node)
    {
        defaultCase(node);
    }

    public void caseTColset(TColset node)
    {
        defaultCase(node);
    }

    public void caseTColpivot(TColpivot node)
    {
        defaultCase(node);
    }

    public void caseTColfocus(TColfocus node)
    {
        defaultCase(node);
    }

    public void caseTColalias(TColalias node)
    {
        defaultCase(node);
    }

    public void caseTCollink(TCollink node)
    {
        defaultCase(node);
    }

    public void caseTShowHide(TShowHide node)
    {
        defaultCase(node);
    }

    public void caseTMod(TMod node)
    {
        defaultCase(node);
    }

    public void caseTLPar(TLPar node)
    {
        defaultCase(node);
    }

    public void caseTRPar(TRPar node)
    {
        defaultCase(node);
    }

    public void caseTLBracket(TLBracket node)
    {
        defaultCase(node);
    }

    public void caseTRBracket(TRBracket node)
    {
        defaultCase(node);
    }

    public void caseTLCurlBracket(TLCurlBracket node)
    {
        defaultCase(node);
    }

    public void caseTRCurlBracket(TRCurlBracket node)
    {
        defaultCase(node);
    }

    public void caseTGroup(TGroup node)
    {
        defaultCase(node);
    }

    public void caseTBlank(TBlank node)
    {
        defaultCase(node);
    }

    public void caseTSpace(TSpace node)
    {
        defaultCase(node);
    }

    public void caseTEqual(TEqual node)
    {
        defaultCase(node);
    }

    public void caseTNewline(TNewline node)
    {
        defaultCase(node);
    }

    public void caseTJava(TJava node)
    {
        defaultCase(node);
    }

    public void caseTR(TR node)
    {
        defaultCase(node);
    }

    public void caseTPython(TPython node)
    {
        defaultCase(node);
    }

    public void caseTProc(TProc node)
    {
        defaultCase(node);
    }

    public void caseTThis(TThis node)
    {
        defaultCase(node);
    }

    public void caseTNull(TNull node)
    {
        defaultCase(node);
    }

    public void caseTImportType(TImportType node)
    {
        defaultCase(node);
    }

    public void caseTRelType(TRelType node)
    {
        defaultCase(node);
    }

    public void caseTDataimporttoken(TDataimporttoken node)
    {
        defaultCase(node);
    }

    public void caseTDataremovetoken(TDataremovetoken node)
    {
        defaultCase(node);
    }

    public void caseTLiteral(TLiteral node)
    {
        defaultCase(node);
    }

    public void caseTHelpToken(THelpToken node)
    {
        defaultCase(node);
    }

    public void caseTCodeblock(TCodeblock node)
    {
        defaultCase(node);
    }

    public void caseTWord(TWord node)
    {
        defaultCase(node);
    }

    public void caseTPanelviz(TPanelviz node)
    {
        defaultCase(node);
    }

    public void caseTPanelclone(TPanelclone node)
    {
        defaultCase(node);
    }

    public void caseTPanelclose(TPanelclose node)
    {
        defaultCase(node);
    }

    public void caseTDataframe(TDataframe node)
    {
        defaultCase(node);
    }

    public void caseTFileText(TFileText node)
    {
        defaultCase(node);
    }

    public void caseTPanelcommentremove(TPanelcommentremove node)
    {
        defaultCase(node);
    }

    public void caseTPanelcommentedit(TPanelcommentedit node)
    {
        defaultCase(node);
    }

    public void caseTPanelcommentadd(TPanelcommentadd node)
    {
        defaultCase(node);
    }

    public void caseEOF(EOF node)
    {
        defaultCase(node);
    }

    public void defaultCase(@SuppressWarnings("unused") Node node)
    {
        // do nothing
    }
}

/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.parser;

import prerna.sablecc.node.*;
import prerna.sablecc.analysis.*;

class TokenIndex extends AnalysisAdapter
{
    int index;

    @Override
    public void caseTNumber(@SuppressWarnings("unused") TNumber node)
    {
        this.index = 0;
    }

    @Override
    public void caseTBoolean(@SuppressWarnings("unused") TBoolean node)
    {
        this.index = 1;
    }

    @Override
    public void caseTId(@SuppressWarnings("unused") TId node)
    {
        this.index = 2;
    }

    @Override
    public void caseTDot(@SuppressWarnings("unused") TDot node)
    {
        this.index = 3;
    }

    @Override
    public void caseTSemicolon(@SuppressWarnings("unused") TSemicolon node)
    {
        this.index = 4;
    }

    @Override
    public void caseTColon(@SuppressWarnings("unused") TColon node)
    {
        this.index = 5;
    }

    @Override
    public void caseTPlus(@SuppressWarnings("unused") TPlus node)
    {
        this.index = 6;
    }

    @Override
    public void caseTMinus(@SuppressWarnings("unused") TMinus node)
    {
        this.index = 7;
    }

    @Override
    public void caseTMult(@SuppressWarnings("unused") TMult node)
    {
        this.index = 8;
    }

    @Override
    public void caseTComma(@SuppressWarnings("unused") TComma node)
    {
        this.index = 9;
    }

    @Override
    public void caseTDiv(@SuppressWarnings("unused") TDiv node)
    {
        this.index = 10;
    }

    @Override
    public void caseTCol(@SuppressWarnings("unused") TCol node)
    {
        this.index = 11;
    }

    @Override
    public void caseTComparator(@SuppressWarnings("unused") TComparator node)
    {
        this.index = 12;
    }

    @Override
    public void caseTVizType(@SuppressWarnings("unused") TVizType node)
    {
        this.index = 13;
    }

    @Override
    public void caseTLogOperator(@SuppressWarnings("unused") TLogOperator node)
    {
        this.index = 14;
    }

    @Override
    public void caseTEqual(@SuppressWarnings("unused") TEqual node)
    {
        this.index = 15;
    }

    @Override
    public void caseTColadd(@SuppressWarnings("unused") TColadd node)
    {
        this.index = 16;
    }

    @Override
    public void caseTApi(@SuppressWarnings("unused") TApi node)
    {
        this.index = 17;
    }

    @Override
    public void caseTMath(@SuppressWarnings("unused") TMath node)
    {
        this.index = 18;
    }

    @Override
    public void caseTColjoin(@SuppressWarnings("unused") TColjoin node)
    {
        this.index = 19;
    }

    @Override
    public void caseTColprefix(@SuppressWarnings("unused") TColprefix node)
    {
        this.index = 20;
    }

    @Override
    public void caseTTablePrefix(@SuppressWarnings("unused") TTablePrefix node)
    {
        this.index = 21;
    }

    @Override
    public void caseTValprefix(@SuppressWarnings("unused") TValprefix node)
    {
        this.index = 22;
    }

    @Override
    public void caseTColremove(@SuppressWarnings("unused") TColremove node)
    {
        this.index = 23;
    }

    @Override
    public void caseTColfilter(@SuppressWarnings("unused") TColfilter node)
    {
        this.index = 24;
    }

    @Override
    public void caseTColunfilter(@SuppressWarnings("unused") TColunfilter node)
    {
        this.index = 25;
    }

    @Override
    public void caseTColfiltermodel(@SuppressWarnings("unused") TColfiltermodel node)
    {
        this.index = 26;
    }

    @Override
    public void caseTColimport(@SuppressWarnings("unused") TColimport node)
    {
        this.index = 27;
    }

    @Override
    public void caseTColset(@SuppressWarnings("unused") TColset node)
    {
        this.index = 28;
    }

    @Override
    public void caseTColpivot(@SuppressWarnings("unused") TColpivot node)
    {
        this.index = 29;
    }

    @Override
    public void caseTColfocus(@SuppressWarnings("unused") TColfocus node)
    {
        this.index = 30;
    }

    @Override
    public void caseTColalias(@SuppressWarnings("unused") TColalias node)
    {
        this.index = 31;
    }

    @Override
    public void caseTColrename(@SuppressWarnings("unused") TColrename node)
    {
        this.index = 32;
    }

    @Override
    public void caseTColsplit(@SuppressWarnings("unused") TColsplit node)
    {
        this.index = 33;
    }

    @Override
    public void caseTCollink(@SuppressWarnings("unused") TCollink node)
    {
        this.index = 34;
    }

    @Override
    public void caseTShowHide(@SuppressWarnings("unused") TShowHide node)
    {
        this.index = 35;
    }

    @Override
    public void caseTMod(@SuppressWarnings("unused") TMod node)
    {
        this.index = 36;
    }

    @Override
    public void caseTLPar(@SuppressWarnings("unused") TLPar node)
    {
        this.index = 37;
    }

    @Override
    public void caseTRPar(@SuppressWarnings("unused") TRPar node)
    {
        this.index = 38;
    }

    @Override
    public void caseTLBracket(@SuppressWarnings("unused") TLBracket node)
    {
        this.index = 39;
    }

    @Override
    public void caseTRBracket(@SuppressWarnings("unused") TRBracket node)
    {
        this.index = 40;
    }

    @Override
    public void caseTLCurlBracket(@SuppressWarnings("unused") TLCurlBracket node)
    {
        this.index = 41;
    }

    @Override
    public void caseTRCurlBracket(@SuppressWarnings("unused") TRCurlBracket node)
    {
        this.index = 42;
    }

    @Override
    public void caseTGroup(@SuppressWarnings("unused") TGroup node)
    {
        this.index = 43;
    }

    @Override
    public void caseTSpace(@SuppressWarnings("unused") TSpace node)
    {
        this.index = 44;
    }

    @Override
    public void caseTNewline(@SuppressWarnings("unused") TNewline node)
    {
        this.index = 45;
    }

    @Override
    public void caseTJava(@SuppressWarnings("unused") TJava node)
    {
        this.index = 46;
    }

    @Override
    public void caseTPython(@SuppressWarnings("unused") TPython node)
    {
        this.index = 47;
    }

    @Override
    public void caseTProc(@SuppressWarnings("unused") TProc node)
    {
        this.index = 48;
    }

    @Override
    public void caseTThis(@SuppressWarnings("unused") TThis node)
    {
        this.index = 49;
    }

    @Override
    public void caseTNull(@SuppressWarnings("unused") TNull node)
    {
        this.index = 50;
    }

    @Override
    public void caseTImportType(@SuppressWarnings("unused") TImportType node)
    {
        this.index = 51;
    }

    @Override
    public void caseTRelType(@SuppressWarnings("unused") TRelType node)
    {
        this.index = 52;
    }

    @Override
    public void caseTDataimporttoken(@SuppressWarnings("unused") TDataimporttoken node)
    {
        this.index = 53;
    }

    @Override
    public void caseTDataremovetoken(@SuppressWarnings("unused") TDataremovetoken node)
    {
        this.index = 54;
    }

    @Override
    public void caseTDataopentoken(@SuppressWarnings("unused") TDataopentoken node)
    {
        this.index = 55;
    }

    @Override
    public void caseTDataoutputtoken(@SuppressWarnings("unused") TDataoutputtoken node)
    {
        this.index = 56;
    }

    @Override
    public void caseTDataquerytoken(@SuppressWarnings("unused") TDataquerytoken node)
    {
        this.index = 57;
    }

    @Override
    public void caseTDatamodeltoken(@SuppressWarnings("unused") TDatamodeltoken node)
    {
        this.index = 58;
    }

    @Override
    public void caseTLiteral(@SuppressWarnings("unused") TLiteral node)
    {
        this.index = 59;
    }

    @Override
    public void caseTHelpToken(@SuppressWarnings("unused") THelpToken node)
    {
        this.index = 60;
    }

    @Override
    public void caseTCodeblock(@SuppressWarnings("unused") TCodeblock node)
    {
        this.index = 61;
    }

    @Override
    public void caseTJsonblock(@SuppressWarnings("unused") TJsonblock node)
    {
        this.index = 62;
    }

    @Override
    public void caseTWord(@SuppressWarnings("unused") TWord node)
    {
        this.index = 63;
    }

    @Override
    public void caseTPanelviz(@SuppressWarnings("unused") TPanelviz node)
    {
        this.index = 64;
    }

    @Override
    public void caseTPanelclone(@SuppressWarnings("unused") TPanelclone node)
    {
        this.index = 65;
    }

    @Override
    public void caseTPanelclose(@SuppressWarnings("unused") TPanelclose node)
    {
        this.index = 66;
    }

    @Override
    public void caseTDataframe(@SuppressWarnings("unused") TDataframe node)
    {
        this.index = 67;
    }

    @Override
    public void caseTDataframeheader(@SuppressWarnings("unused") TDataframeheader node)
    {
        this.index = 68;
    }

    @Override
    public void caseTDataframeduplicates(@SuppressWarnings("unused") TDataframeduplicates node)
    {
        this.index = 69;
    }

    @Override
    public void caseTFileText(@SuppressWarnings("unused") TFileText node)
    {
        this.index = 70;
    }

    @Override
    public void caseTPanelcommentremove(@SuppressWarnings("unused") TPanelcommentremove node)
    {
        this.index = 71;
    }

    @Override
    public void caseTPanelcommentedit(@SuppressWarnings("unused") TPanelcommentedit node)
    {
        this.index = 72;
    }

    @Override
    public void caseTPanelcommentadd(@SuppressWarnings("unused") TPanelcommentadd node)
    {
        this.index = 73;
    }

    @Override
    public void caseTPanellookandfeel(@SuppressWarnings("unused") TPanellookandfeel node)
    {
        this.index = 74;
    }

    @Override
    public void caseTPaneltools(@SuppressWarnings("unused") TPaneltools node)
    {
        this.index = 75;
    }

    @Override
    public void caseTPanelconfig(@SuppressWarnings("unused") TPanelconfig node)
    {
        this.index = 76;
    }

    @Override
    public void caseTOutputToken(@SuppressWarnings("unused") TOutputToken node)
    {
        this.index = 77;
    }

    @Override
    public void caseTUserinput(@SuppressWarnings("unused") TUserinput node)
    {
        this.index = 78;
    }

    @Override
    public void caseTJoin(@SuppressWarnings("unused") TJoin node)
    {
        this.index = 79;
    }

    @Override
    public void caseTDatatypeToken(@SuppressWarnings("unused") TDatatypeToken node)
    {
        this.index = 80;
    }

    @Override
    public void caseTDataconnectToken(@SuppressWarnings("unused") TDataconnectToken node)
    {
        this.index = 81;
    }

    @Override
    public void caseTDatanetworkconnectToken(@SuppressWarnings("unused") TDatanetworkconnectToken node)
    {
        this.index = 82;
    }

    @Override
    public void caseTDatanetworkdisconnectToken(@SuppressWarnings("unused") TDatanetworkdisconnectToken node)
    {
        this.index = 83;
    }

    @Override
    public void caseTDataconnectdbToken(@SuppressWarnings("unused") TDataconnectdbToken node)
    {
        this.index = 84;
    }

    @Override
    public void caseTDatabaselistToken(@SuppressWarnings("unused") TDatabaselistToken node)
    {
        this.index = 85;
    }

    @Override
    public void caseTDatabaseconceptsToken(@SuppressWarnings("unused") TDatabaseconceptsToken node)
    {
        this.index = 86;
    }

    @Override
    public void caseTDatabaseconceptpropertiesToken(@SuppressWarnings("unused") TDatabaseconceptpropertiesToken node)
    {
        this.index = 87;
    }

    @Override
    public void caseTDatabasemetamodelToken(@SuppressWarnings("unused") TDatabasemetamodelToken node)
    {
        this.index = 88;
    }

    @Override
    public void caseTDashboardconfig(@SuppressWarnings("unused") TDashboardconfig node)
    {
        this.index = 89;
    }

    @Override
    public void caseTDashboardAddToken(@SuppressWarnings("unused") TDashboardAddToken node)
    {
        this.index = 90;
    }

    @Override
    public void caseTWherestr(@SuppressWarnings("unused") TWherestr node)
    {
        this.index = 91;
    }

    @Override
    public void caseEOF(@SuppressWarnings("unused") EOF node)
    {
        this.index = 92;
    }
}

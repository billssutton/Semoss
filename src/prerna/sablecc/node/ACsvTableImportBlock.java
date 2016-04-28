/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ACsvTableImportBlock extends PImportBlock
{
    private PCsvTable _csvTable_;

    public ACsvTableImportBlock()
    {
        // Constructor
    }

    public ACsvTableImportBlock(
        @SuppressWarnings("hiding") PCsvTable _csvTable_)
    {
        // Constructor
        setCsvTable(_csvTable_);

    }

    @Override
    public Object clone()
    {
        return new ACsvTableImportBlock(
            cloneNode(this._csvTable_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseACsvTableImportBlock(this);
    }

    public PCsvTable getCsvTable()
    {
        return this._csvTable_;
    }

    public void setCsvTable(PCsvTable node)
    {
        if(this._csvTable_ != null)
        {
            this._csvTable_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._csvTable_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._csvTable_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._csvTable_ == child)
        {
            this._csvTable_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._csvTable_ == oldChild)
        {
            setCsvTable((PCsvTable) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

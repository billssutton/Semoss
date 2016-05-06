/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AColTerm extends PTerm
{
    private PColDef _col_;

    public AColTerm()
    {
        // Constructor
    }

    public AColTerm(
        @SuppressWarnings("hiding") PColDef _col_)
    {
        // Constructor
        setCol(_col_);

    }

    @Override
    public Object clone()
    {
        return new AColTerm(
            cloneNode(this._col_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAColTerm(this);
    }

    public PColDef getCol()
    {
        return this._col_;
    }

    public void setCol(PColDef node)
    {
        if(this._col_ != null)
        {
            this._col_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._col_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._col_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._col_ == child)
        {
            this._col_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._col_ == oldChild)
        {
            setCol((PColDef) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

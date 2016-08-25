/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AVarTerm extends PTerm
{
    private PVarDef _var_;

    public AVarTerm()
    {
        // Constructor
    }

    public AVarTerm(
        @SuppressWarnings("hiding") PVarDef _var_)
    {
        // Constructor
        setVar(_var_);

    }

    @Override
    public Object clone()
    {
        return new AVarTerm(
            cloneNode(this._var_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAVarTerm(this);
    }

    public PVarDef getVar()
    {
        return this._var_;
    }

    public void setVar(PVarDef node)
    {
        if(this._var_ != null)
        {
            this._var_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._var_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._var_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._var_ == child)
        {
            this._var_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._var_ == oldChild)
        {
            setVar((PVarDef) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

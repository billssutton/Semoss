/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ADataconnectdbDataop extends PDataop
{
    private PDataconnectdb _dataconnectdb_;

    public ADataconnectdbDataop()
    {
        // Constructor
    }

    public ADataconnectdbDataop(
        @SuppressWarnings("hiding") PDataconnectdb _dataconnectdb_)
    {
        // Constructor
        setDataconnectdb(_dataconnectdb_);

    }

    @Override
    public Object clone()
    {
        return new ADataconnectdbDataop(
            cloneNode(this._dataconnectdb_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseADataconnectdbDataop(this);
    }

    public PDataconnectdb getDataconnectdb()
    {
        return this._dataconnectdb_;
    }

    public void setDataconnectdb(PDataconnectdb node)
    {
        if(this._dataconnectdb_ != null)
        {
            this._dataconnectdb_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._dataconnectdb_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._dataconnectdb_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._dataconnectdb_ == child)
        {
            this._dataconnectdb_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._dataconnectdb_ == oldChild)
        {
            setDataconnectdb((PDataconnectdb) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

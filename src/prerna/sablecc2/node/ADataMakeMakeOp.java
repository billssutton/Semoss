/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class ADataMakeMakeOp extends PMakeOp
{
    private PMakeData _makeData_;

    public ADataMakeMakeOp()
    {
        // Constructor
    }

    public ADataMakeMakeOp(
        @SuppressWarnings("hiding") PMakeData _makeData_)
    {
        // Constructor
        setMakeData(_makeData_);

    }

    @Override
    public Object clone()
    {
        return new ADataMakeMakeOp(
            cloneNode(this._makeData_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseADataMakeMakeOp(this);
    }

    public PMakeData getMakeData()
    {
        return this._makeData_;
    }

    public void setMakeData(PMakeData node)
    {
        if(this._makeData_ != null)
        {
            this._makeData_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._makeData_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._makeData_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._makeData_ == child)
        {
            this._makeData_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._makeData_ == oldChild)
        {
            setMakeData((PMakeData) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

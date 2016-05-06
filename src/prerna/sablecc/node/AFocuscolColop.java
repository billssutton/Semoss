/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AFocuscolColop extends PColop
{
    private PFocusColumn _focusColumn_;

    public AFocuscolColop()
    {
        // Constructor
    }

    public AFocuscolColop(
        @SuppressWarnings("hiding") PFocusColumn _focusColumn_)
    {
        // Constructor
        setFocusColumn(_focusColumn_);

    }

    @Override
    public Object clone()
    {
        return new AFocuscolColop(
            cloneNode(this._focusColumn_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAFocuscolColop(this);
    }

    public PFocusColumn getFocusColumn()
    {
        return this._focusColumn_;
    }

    public void setFocusColumn(PFocusColumn node)
    {
        if(this._focusColumn_ != null)
        {
            this._focusColumn_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._focusColumn_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._focusColumn_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._focusColumn_ == child)
        {
            this._focusColumn_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._focusColumn_ == oldChild)
        {
            setFocusColumn((PFocusColumn) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

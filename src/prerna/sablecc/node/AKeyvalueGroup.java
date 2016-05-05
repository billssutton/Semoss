/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AKeyvalueGroup extends PKeyvalueGroup
{
    private TComma _comma_;
    private PKeyvalue _keyvalue_;

    public AKeyvalueGroup()
    {
        // Constructor
    }

    public AKeyvalueGroup(
        @SuppressWarnings("hiding") TComma _comma_,
        @SuppressWarnings("hiding") PKeyvalue _keyvalue_)
    {
        // Constructor
        setComma(_comma_);

        setKeyvalue(_keyvalue_);

    }

    @Override
    public Object clone()
    {
        return new AKeyvalueGroup(
            cloneNode(this._comma_),
            cloneNode(this._keyvalue_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAKeyvalueGroup(this);
    }

    public TComma getComma()
    {
        return this._comma_;
    }

    public void setComma(TComma node)
    {
        if(this._comma_ != null)
        {
            this._comma_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._comma_ = node;
    }

    public PKeyvalue getKeyvalue()
    {
        return this._keyvalue_;
    }

    public void setKeyvalue(PKeyvalue node)
    {
        if(this._keyvalue_ != null)
        {
            this._keyvalue_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._keyvalue_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._comma_)
            + toString(this._keyvalue_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._comma_ == child)
        {
            this._comma_ = null;
            return;
        }

        if(this._keyvalue_ == child)
        {
            this._keyvalue_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._comma_ == oldChild)
        {
            setComma((TComma) newChild);
            return;
        }

        if(this._keyvalue_ == oldChild)
        {
            setKeyvalue((PKeyvalue) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

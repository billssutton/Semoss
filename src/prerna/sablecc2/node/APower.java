/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class APower extends PPower
{
    private PExprComponent _base_;
    private TPow _pow_;
    private PTerm _exponent_;

    public APower()
    {
        // Constructor
    }

    public APower(
        @SuppressWarnings("hiding") PExprComponent _base_,
        @SuppressWarnings("hiding") TPow _pow_,
        @SuppressWarnings("hiding") PTerm _exponent_)
    {
        // Constructor
        setBase(_base_);

        setPow(_pow_);

        setExponent(_exponent_);

    }

    @Override
    public Object clone()
    {
        return new APower(
            cloneNode(this._base_),
            cloneNode(this._pow_),
            cloneNode(this._exponent_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAPower(this);
    }

    public PExprComponent getBase()
    {
        return this._base_;
    }

    public void setBase(PExprComponent node)
    {
        if(this._base_ != null)
        {
            this._base_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._base_ = node;
    }

    public TPow getPow()
    {
        return this._pow_;
    }

    public void setPow(TPow node)
    {
        if(this._pow_ != null)
        {
            this._pow_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._pow_ = node;
    }

    public PTerm getExponent()
    {
        return this._exponent_;
    }

    public void setExponent(PTerm node)
    {
        if(this._exponent_ != null)
        {
            this._exponent_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._exponent_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._base_)
            + toString(this._pow_)
            + toString(this._exponent_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._base_ == child)
        {
            this._base_ = null;
            return;
        }

        if(this._pow_ == child)
        {
            this._pow_ = null;
            return;
        }

        if(this._exponent_ == child)
        {
            this._exponent_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._base_ == oldChild)
        {
            setBase((PExprComponent) newChild);
            return;
        }

        if(this._pow_ == oldChild)
        {
            setPow((TPow) newChild);
            return;
        }

        if(this._exponent_ == oldChild)
        {
            setExponent((PTerm) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

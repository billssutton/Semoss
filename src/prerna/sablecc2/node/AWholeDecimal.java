/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AWholeDecimal extends PWholeDecimal
{
    private TNumber _whole_;
    private TDot _dot_;
    private TNumber _fraction_;

    public AWholeDecimal()
    {
        // Constructor
    }

    public AWholeDecimal(
        @SuppressWarnings("hiding") TNumber _whole_,
        @SuppressWarnings("hiding") TDot _dot_,
        @SuppressWarnings("hiding") TNumber _fraction_)
    {
        // Constructor
        setWhole(_whole_);

        setDot(_dot_);

        setFraction(_fraction_);

    }

    @Override
    public Object clone()
    {
        return new AWholeDecimal(
            cloneNode(this._whole_),
            cloneNode(this._dot_),
            cloneNode(this._fraction_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAWholeDecimal(this);
    }

    public TNumber getWhole()
    {
        return this._whole_;
    }

    public void setWhole(TNumber node)
    {
        if(this._whole_ != null)
        {
            this._whole_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._whole_ = node;
    }

    public TDot getDot()
    {
        return this._dot_;
    }

    public void setDot(TDot node)
    {
        if(this._dot_ != null)
        {
            this._dot_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._dot_ = node;
    }

    public TNumber getFraction()
    {
        return this._fraction_;
    }

    public void setFraction(TNumber node)
    {
        if(this._fraction_ != null)
        {
            this._fraction_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._fraction_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._whole_)
            + toString(this._dot_)
            + toString(this._fraction_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._whole_ == child)
        {
            this._whole_ = null;
            return;
        }

        if(this._dot_ == child)
        {
            this._dot_ = null;
            return;
        }

        if(this._fraction_ == child)
        {
            this._fraction_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._whole_ == oldChild)
        {
            setWhole((TNumber) newChild);
            return;
        }

        if(this._dot_ == oldChild)
        {
            setDot((TDot) newChild);
            return;
        }

        if(this._fraction_ == oldChild)
        {
            setFraction((TNumber) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

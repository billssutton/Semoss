/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ADivExpr extends PExpr
{
    private PTerm _left_;
    private TDiv _div_;
    private PTerm _right_;

    public ADivExpr()
    {
        // Constructor
    }

    public ADivExpr(
        @SuppressWarnings("hiding") PTerm _left_,
        @SuppressWarnings("hiding") TDiv _div_,
        @SuppressWarnings("hiding") PTerm _right_)
    {
        // Constructor
        setLeft(_left_);

        setDiv(_div_);

        setRight(_right_);

    }

    @Override
    public Object clone()
    {
        return new ADivExpr(
            cloneNode(this._left_),
            cloneNode(this._div_),
            cloneNode(this._right_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseADivExpr(this);
    }

    public PTerm getLeft()
    {
        return this._left_;
    }

    public void setLeft(PTerm node)
    {
        if(this._left_ != null)
        {
            this._left_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._left_ = node;
    }

    public TDiv getDiv()
    {
        return this._div_;
    }

    public void setDiv(TDiv node)
    {
        if(this._div_ != null)
        {
            this._div_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._div_ = node;
    }

    public PTerm getRight()
    {
        return this._right_;
    }

    public void setRight(PTerm node)
    {
        if(this._right_ != null)
        {
            this._right_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._right_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._left_)
            + toString(this._div_)
            + toString(this._right_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._left_ == child)
        {
            this._left_ = null;
            return;
        }

        if(this._div_ == child)
        {
            this._div_ = null;
            return;
        }

        if(this._right_ == child)
        {
            this._right_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._left_ == oldChild)
        {
            setLeft((PTerm) newChild);
            return;
        }

        if(this._div_ == oldChild)
        {
            setDiv((TDiv) newChild);
            return;
        }

        if(this._right_ == oldChild)
        {
            setRight((PTerm) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class ABaseSubExpr extends PBaseSubExpr
{
    private PMasterExpr _masterExpr_;
    private TSemicolon _semicolon_;

    public ABaseSubExpr()
    {
        // Constructor
    }

    public ABaseSubExpr(
        @SuppressWarnings("hiding") PMasterExpr _masterExpr_,
        @SuppressWarnings("hiding") TSemicolon _semicolon_)
    {
        // Constructor
        setMasterExpr(_masterExpr_);

        setSemicolon(_semicolon_);

    }

    @Override
    public Object clone()
    {
        return new ABaseSubExpr(
            cloneNode(this._masterExpr_),
            cloneNode(this._semicolon_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseABaseSubExpr(this);
    }

    public PMasterExpr getMasterExpr()
    {
        return this._masterExpr_;
    }

    public void setMasterExpr(PMasterExpr node)
    {
        if(this._masterExpr_ != null)
        {
            this._masterExpr_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._masterExpr_ = node;
    }

    public TSemicolon getSemicolon()
    {
        return this._semicolon_;
    }

    public void setSemicolon(TSemicolon node)
    {
        if(this._semicolon_ != null)
        {
            this._semicolon_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._semicolon_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._masterExpr_)
            + toString(this._semicolon_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._masterExpr_ == child)
        {
            this._masterExpr_ = null;
            return;
        }

        if(this._semicolon_ == child)
        {
            this._semicolon_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._masterExpr_ == oldChild)
        {
            setMasterExpr((PMasterExpr) newChild);
            return;
        }

        if(this._semicolon_ == oldChild)
        {
            setSemicolon((TSemicolon) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

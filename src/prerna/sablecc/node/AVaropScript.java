/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AVaropScript extends PScript
{
    private PVarop _varop_;
    private TSemicolon _semicolon_;

    public AVaropScript()
    {
        // Constructor
    }

    public AVaropScript(
        @SuppressWarnings("hiding") PVarop _varop_,
        @SuppressWarnings("hiding") TSemicolon _semicolon_)
    {
        // Constructor
        setVarop(_varop_);

        setSemicolon(_semicolon_);

    }

    @Override
    public Object clone()
    {
        return new AVaropScript(
            cloneNode(this._varop_),
            cloneNode(this._semicolon_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAVaropScript(this);
    }

    public PVarop getVarop()
    {
        return this._varop_;
    }

    public void setVarop(PVarop node)
    {
        if(this._varop_ != null)
        {
            this._varop_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._varop_ = node;
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
            + toString(this._varop_)
            + toString(this._semicolon_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._varop_ == child)
        {
            this._varop_ = null;
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
        if(this._varop_ == oldChild)
        {
            setVarop((PVarop) newChild);
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

/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AMetaScriptMetaRoutine extends PMetaRoutine
{
    private TMeta _meta_;
    private TCustom _custom_;
    private PScript _script_;

    public AMetaScriptMetaRoutine()
    {
        // Constructor
    }

    public AMetaScriptMetaRoutine(
        @SuppressWarnings("hiding") TMeta _meta_,
        @SuppressWarnings("hiding") TCustom _custom_,
        @SuppressWarnings("hiding") PScript _script_)
    {
        // Constructor
        setMeta(_meta_);

        setCustom(_custom_);

        setScript(_script_);

    }

    @Override
    public Object clone()
    {
        return new AMetaScriptMetaRoutine(
            cloneNode(this._meta_),
            cloneNode(this._custom_),
            cloneNode(this._script_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAMetaScriptMetaRoutine(this);
    }

    public TMeta getMeta()
    {
        return this._meta_;
    }

    public void setMeta(TMeta node)
    {
        if(this._meta_ != null)
        {
            this._meta_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._meta_ = node;
    }

    public TCustom getCustom()
    {
        return this._custom_;
    }

    public void setCustom(TCustom node)
    {
        if(this._custom_ != null)
        {
            this._custom_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._custom_ = node;
    }

    public PScript getScript()
    {
        return this._script_;
    }

    public void setScript(PScript node)
    {
        if(this._script_ != null)
        {
            this._script_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._script_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._meta_)
            + toString(this._custom_)
            + toString(this._script_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._meta_ == child)
        {
            this._meta_ = null;
            return;
        }

        if(this._custom_ == child)
        {
            this._custom_ = null;
            return;
        }

        if(this._script_ == child)
        {
            this._script_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._meta_ == oldChild)
        {
            setMeta((TMeta) newChild);
            return;
        }

        if(this._custom_ == oldChild)
        {
            setCustom((TCustom) newChild);
            return;
        }

        if(this._script_ == oldChild)
        {
            setScript((PScript) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

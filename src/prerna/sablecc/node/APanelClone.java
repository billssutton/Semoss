/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class APanelClone extends PPanelClone
{
    private TPanelclone _panelclone_;
    private TLPar _lPar_;
    private TNumber _newid_;
    private TRPar _rPar_;

    public APanelClone()
    {
        // Constructor
    }

    public APanelClone(
        @SuppressWarnings("hiding") TPanelclone _panelclone_,
        @SuppressWarnings("hiding") TLPar _lPar_,
        @SuppressWarnings("hiding") TNumber _newid_,
        @SuppressWarnings("hiding") TRPar _rPar_)
    {
        // Constructor
        setPanelclone(_panelclone_);

        setLPar(_lPar_);

        setNewid(_newid_);

        setRPar(_rPar_);

    }

    @Override
    public Object clone()
    {
        return new APanelClone(
            cloneNode(this._panelclone_),
            cloneNode(this._lPar_),
            cloneNode(this._newid_),
            cloneNode(this._rPar_));
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAPanelClone(this);
    }

    public TPanelclone getPanelclone()
    {
        return this._panelclone_;
    }

    public void setPanelclone(TPanelclone node)
    {
        if(this._panelclone_ != null)
        {
            this._panelclone_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._panelclone_ = node;
    }

    public TLPar getLPar()
    {
        return this._lPar_;
    }

    public void setLPar(TLPar node)
    {
        if(this._lPar_ != null)
        {
            this._lPar_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._lPar_ = node;
    }

    public TNumber getNewid()
    {
        return this._newid_;
    }

    public void setNewid(TNumber node)
    {
        if(this._newid_ != null)
        {
            this._newid_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._newid_ = node;
    }

    public TRPar getRPar()
    {
        return this._rPar_;
    }

    public void setRPar(TRPar node)
    {
        if(this._rPar_ != null)
        {
            this._rPar_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._rPar_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._panelclone_)
            + toString(this._lPar_)
            + toString(this._newid_)
            + toString(this._rPar_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._panelclone_ == child)
        {
            this._panelclone_ = null;
            return;
        }

        if(this._lPar_ == child)
        {
            this._lPar_ = null;
            return;
        }

        if(this._newid_ == child)
        {
            this._newid_ = null;
            return;
        }

        if(this._rPar_ == child)
        {
            this._rPar_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._panelclone_ == oldChild)
        {
            setPanelclone((TPanelclone) newChild);
            return;
        }

        if(this._lPar_ == oldChild)
        {
            setLPar((TLPar) newChild);
            return;
        }

        if(this._newid_ == oldChild)
        {
            setNewid((TNumber) newChild);
            return;
        }

        if(this._rPar_ == oldChild)
        {
            setRPar((TRPar) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

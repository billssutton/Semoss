/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import java.util.*;
import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AMapOp extends PMapOp
{
    private TMap _map_;
    private TLPar _lPar_;
    private PPimport _pimport_;
    private TCodeblock _codeblock_;
    private final LinkedList<PPnoun> _pnoun_ = new LinkedList<PPnoun>();
    private TRPar _rPar_;

    public AMapOp()
    {
        // Constructor
    }

    public AMapOp(
        @SuppressWarnings("hiding") TMap _map_,
        @SuppressWarnings("hiding") TLPar _lPar_,
        @SuppressWarnings("hiding") PPimport _pimport_,
        @SuppressWarnings("hiding") TCodeblock _codeblock_,
        @SuppressWarnings("hiding") List<?> _pnoun_,
        @SuppressWarnings("hiding") TRPar _rPar_)
    {
        // Constructor
        setMap(_map_);

        setLPar(_lPar_);

        setPimport(_pimport_);

        setCodeblock(_codeblock_);

        setPnoun(_pnoun_);

        setRPar(_rPar_);

    }

    @Override
    public Object clone()
    {
        return new AMapOp(
            cloneNode(this._map_),
            cloneNode(this._lPar_),
            cloneNode(this._pimport_),
            cloneNode(this._codeblock_),
            cloneList(this._pnoun_),
            cloneNode(this._rPar_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAMapOp(this);
    }

    public TMap getMap()
    {
        return this._map_;
    }

    public void setMap(TMap node)
    {
        if(this._map_ != null)
        {
            this._map_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._map_ = node;
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

    public PPimport getPimport()
    {
        return this._pimport_;
    }

    public void setPimport(PPimport node)
    {
        if(this._pimport_ != null)
        {
            this._pimport_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._pimport_ = node;
    }

    public TCodeblock getCodeblock()
    {
        return this._codeblock_;
    }

    public void setCodeblock(TCodeblock node)
    {
        if(this._codeblock_ != null)
        {
            this._codeblock_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._codeblock_ = node;
    }

    public LinkedList<PPnoun> getPnoun()
    {
        return this._pnoun_;
    }

    public void setPnoun(List<?> list)
    {
        for(PPnoun e : this._pnoun_)
        {
            e.parent(null);
        }
        this._pnoun_.clear();

        for(Object obj_e : list)
        {
            PPnoun e = (PPnoun) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._pnoun_.add(e);
        }
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
            + toString(this._map_)
            + toString(this._lPar_)
            + toString(this._pimport_)
            + toString(this._codeblock_)
            + toString(this._pnoun_)
            + toString(this._rPar_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._map_ == child)
        {
            this._map_ = null;
            return;
        }

        if(this._lPar_ == child)
        {
            this._lPar_ = null;
            return;
        }

        if(this._pimport_ == child)
        {
            this._pimport_ = null;
            return;
        }

        if(this._codeblock_ == child)
        {
            this._codeblock_ = null;
            return;
        }

        if(this._pnoun_.remove(child))
        {
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
        if(this._map_ == oldChild)
        {
            setMap((TMap) newChild);
            return;
        }

        if(this._lPar_ == oldChild)
        {
            setLPar((TLPar) newChild);
            return;
        }

        if(this._pimport_ == oldChild)
        {
            setPimport((PPimport) newChild);
            return;
        }

        if(this._codeblock_ == oldChild)
        {
            setCodeblock((TCodeblock) newChild);
            return;
        }

        for(ListIterator<PPnoun> i = this._pnoun_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PPnoun) newChild);
                    newChild.parent(this);
                    oldChild.parent(null);
                    return;
                }

                i.remove();
                oldChild.parent(null);
                return;
            }
        }

        if(this._rPar_ == oldChild)
        {
            setRPar((TRPar) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

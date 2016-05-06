/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AGroupBy extends PGroupBy
{
    private TLPar _lPar_;
    private PColDef _colDef_;
    private final LinkedList<PColGroup> _colGroup_ = new LinkedList<PColGroup>();
    private TRPar _rPar_;

    public AGroupBy()
    {
        // Constructor
    }

    public AGroupBy(
        @SuppressWarnings("hiding") TLPar _lPar_,
        @SuppressWarnings("hiding") PColDef _colDef_,
        @SuppressWarnings("hiding") List<?> _colGroup_,
        @SuppressWarnings("hiding") TRPar _rPar_)
    {
        // Constructor
        setLPar(_lPar_);

        setColDef(_colDef_);

        setColGroup(_colGroup_);

        setRPar(_rPar_);

    }

    @Override
    public Object clone()
    {
        return new AGroupBy(
            cloneNode(this._lPar_),
            cloneNode(this._colDef_),
            cloneList(this._colGroup_),
            cloneNode(this._rPar_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAGroupBy(this);
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

    public PColDef getColDef()
    {
        return this._colDef_;
    }

    public void setColDef(PColDef node)
    {
        if(this._colDef_ != null)
        {
            this._colDef_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._colDef_ = node;
    }

    public LinkedList<PColGroup> getColGroup()
    {
        return this._colGroup_;
    }

    public void setColGroup(List<?> list)
    {
        for(PColGroup e : this._colGroup_)
        {
            e.parent(null);
        }
        this._colGroup_.clear();

        for(Object obj_e : list)
        {
            PColGroup e = (PColGroup) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._colGroup_.add(e);
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
            + toString(this._lPar_)
            + toString(this._colDef_)
            + toString(this._colGroup_)
            + toString(this._rPar_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._lPar_ == child)
        {
            this._lPar_ = null;
            return;
        }

        if(this._colDef_ == child)
        {
            this._colDef_ = null;
            return;
        }

        if(this._colGroup_.remove(child))
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
        if(this._lPar_ == oldChild)
        {
            setLPar((TLPar) newChild);
            return;
        }

        if(this._colDef_ == oldChild)
        {
            setColDef((PColDef) newChild);
            return;
        }

        for(ListIterator<PColGroup> i = this._colGroup_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PColGroup) newChild);
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

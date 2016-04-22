/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AWhereClauseNocomma extends PWhereClauseNocomma
{
    private TLPar _lPar_;
    private PColWhere _colWhere_;
    private final LinkedList<PColWhereGroup> _colWhereGroup_ = new LinkedList<PColWhereGroup>();
    private TRPar _rPar_;

    public AWhereClauseNocomma()
    {
        // Constructor
    }

    public AWhereClauseNocomma(
        @SuppressWarnings("hiding") TLPar _lPar_,
        @SuppressWarnings("hiding") PColWhere _colWhere_,
        @SuppressWarnings("hiding") List<?> _colWhereGroup_,
        @SuppressWarnings("hiding") TRPar _rPar_)
    {
        // Constructor
        setLPar(_lPar_);

        setColWhere(_colWhere_);

        setColWhereGroup(_colWhereGroup_);

        setRPar(_rPar_);

    }

    @Override
    public Object clone()
    {
        return new AWhereClauseNocomma(
            cloneNode(this._lPar_),
            cloneNode(this._colWhere_),
            cloneList(this._colWhereGroup_),
            cloneNode(this._rPar_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAWhereClauseNocomma(this);
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

    public PColWhere getColWhere()
    {
        return this._colWhere_;
    }

    public void setColWhere(PColWhere node)
    {
        if(this._colWhere_ != null)
        {
            this._colWhere_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._colWhere_ = node;
    }

    public LinkedList<PColWhereGroup> getColWhereGroup()
    {
        return this._colWhereGroup_;
    }

    public void setColWhereGroup(List<?> list)
    {
        for(PColWhereGroup e : this._colWhereGroup_)
        {
            e.parent(null);
        }
        this._colWhereGroup_.clear();

        for(Object obj_e : list)
        {
            PColWhereGroup e = (PColWhereGroup) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._colWhereGroup_.add(e);
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
            + toString(this._colWhere_)
            + toString(this._colWhereGroup_)
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

        if(this._colWhere_ == child)
        {
            this._colWhere_ = null;
            return;
        }

        if(this._colWhereGroup_.remove(child))
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

        if(this._colWhere_ == oldChild)
        {
            setColWhere((PColWhere) newChild);
            return;
        }

        for(ListIterator<PColWhereGroup> i = this._colWhereGroup_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PColWhereGroup) newChild);
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

/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AMapObjRow extends PMapObjRow
{
    private TLBracket _lBracket_;
    private PWordOrNumOrNestedObj _wordOrNumOrNestedObj_;
    private final LinkedList<PWordOrNumOrNestedObjGroup> _wordOrNumOrNestedObjGroup_ = new LinkedList<PWordOrNumOrNestedObjGroup>();
    private TRBracket _rBracket_;

    public AMapObjRow()
    {
        // Constructor
    }

    public AMapObjRow(
        @SuppressWarnings("hiding") TLBracket _lBracket_,
        @SuppressWarnings("hiding") PWordOrNumOrNestedObj _wordOrNumOrNestedObj_,
        @SuppressWarnings("hiding") List<?> _wordOrNumOrNestedObjGroup_,
        @SuppressWarnings("hiding") TRBracket _rBracket_)
    {
        // Constructor
        setLBracket(_lBracket_);

        setWordOrNumOrNestedObj(_wordOrNumOrNestedObj_);

        setWordOrNumOrNestedObjGroup(_wordOrNumOrNestedObjGroup_);

        setRBracket(_rBracket_);

    }

    @Override
    public Object clone()
    {
        return new AMapObjRow(
            cloneNode(this._lBracket_),
            cloneNode(this._wordOrNumOrNestedObj_),
            cloneList(this._wordOrNumOrNestedObjGroup_),
            cloneNode(this._rBracket_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAMapObjRow(this);
    }

    public TLBracket getLBracket()
    {
        return this._lBracket_;
    }

    public void setLBracket(TLBracket node)
    {
        if(this._lBracket_ != null)
        {
            this._lBracket_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._lBracket_ = node;
    }

    public PWordOrNumOrNestedObj getWordOrNumOrNestedObj()
    {
        return this._wordOrNumOrNestedObj_;
    }

    public void setWordOrNumOrNestedObj(PWordOrNumOrNestedObj node)
    {
        if(this._wordOrNumOrNestedObj_ != null)
        {
            this._wordOrNumOrNestedObj_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._wordOrNumOrNestedObj_ = node;
    }

    public LinkedList<PWordOrNumOrNestedObjGroup> getWordOrNumOrNestedObjGroup()
    {
        return this._wordOrNumOrNestedObjGroup_;
    }

    public void setWordOrNumOrNestedObjGroup(List<?> list)
    {
        for(PWordOrNumOrNestedObjGroup e : this._wordOrNumOrNestedObjGroup_)
        {
            e.parent(null);
        }
        this._wordOrNumOrNestedObjGroup_.clear();

        for(Object obj_e : list)
        {
            PWordOrNumOrNestedObjGroup e = (PWordOrNumOrNestedObjGroup) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._wordOrNumOrNestedObjGroup_.add(e);
        }
    }

    public TRBracket getRBracket()
    {
        return this._rBracket_;
    }

    public void setRBracket(TRBracket node)
    {
        if(this._rBracket_ != null)
        {
            this._rBracket_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._rBracket_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._lBracket_)
            + toString(this._wordOrNumOrNestedObj_)
            + toString(this._wordOrNumOrNestedObjGroup_)
            + toString(this._rBracket_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._lBracket_ == child)
        {
            this._lBracket_ = null;
            return;
        }

        if(this._wordOrNumOrNestedObj_ == child)
        {
            this._wordOrNumOrNestedObj_ = null;
            return;
        }

        if(this._wordOrNumOrNestedObjGroup_.remove(child))
        {
            return;
        }

        if(this._rBracket_ == child)
        {
            this._rBracket_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._lBracket_ == oldChild)
        {
            setLBracket((TLBracket) newChild);
            return;
        }

        if(this._wordOrNumOrNestedObj_ == oldChild)
        {
            setWordOrNumOrNestedObj((PWordOrNumOrNestedObj) newChild);
            return;
        }

        for(ListIterator<PWordOrNumOrNestedObjGroup> i = this._wordOrNumOrNestedObjGroup_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PWordOrNumOrNestedObjGroup) newChild);
                    newChild.parent(this);
                    oldChild.parent(null);
                    return;
                }

                i.remove();
                oldChild.parent(null);
                return;
            }
        }

        if(this._rBracket_ == oldChild)
        {
            setRBracket((TRBracket) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ACsvRow extends PCsvRow
{
    private TLBracket _lBracket_;
    private PWordOrNum _wordOrNum_;
    private final LinkedList<PCsvGroup> _csvGroup_ = new LinkedList<PCsvGroup>();
    private TRBracket _rBracket_;

    public ACsvRow()
    {
        // Constructor
    }

    public ACsvRow(
        @SuppressWarnings("hiding") TLBracket _lBracket_,
        @SuppressWarnings("hiding") PWordOrNum _wordOrNum_,
        @SuppressWarnings("hiding") List<?> _csvGroup_,
        @SuppressWarnings("hiding") TRBracket _rBracket_)
    {
        // Constructor
        setLBracket(_lBracket_);

        setWordOrNum(_wordOrNum_);

        setCsvGroup(_csvGroup_);

        setRBracket(_rBracket_);

    }

    @Override
    public Object clone()
    {
        return new ACsvRow(
            cloneNode(this._lBracket_),
            cloneNode(this._wordOrNum_),
            cloneList(this._csvGroup_),
            cloneNode(this._rBracket_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseACsvRow(this);
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

    public PWordOrNum getWordOrNum()
    {
        return this._wordOrNum_;
    }

    public void setWordOrNum(PWordOrNum node)
    {
        if(this._wordOrNum_ != null)
        {
            this._wordOrNum_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._wordOrNum_ = node;
    }

    public LinkedList<PCsvGroup> getCsvGroup()
    {
        return this._csvGroup_;
    }

    public void setCsvGroup(List<?> list)
    {
        for(PCsvGroup e : this._csvGroup_)
        {
            e.parent(null);
        }
        this._csvGroup_.clear();

        for(Object obj_e : list)
        {
            PCsvGroup e = (PCsvGroup) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._csvGroup_.add(e);
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
            + toString(this._wordOrNum_)
            + toString(this._csvGroup_)
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

        if(this._wordOrNum_ == child)
        {
            this._wordOrNum_ = null;
            return;
        }

        if(this._csvGroup_.remove(child))
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

        if(this._wordOrNum_ == oldChild)
        {
            setWordOrNum((PWordOrNum) newChild);
            return;
        }

        for(ListIterator<PCsvGroup> i = this._csvGroup_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PCsvGroup) newChild);
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

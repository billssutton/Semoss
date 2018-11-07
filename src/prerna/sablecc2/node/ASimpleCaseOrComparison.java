/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import java.util.*;
import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class ASimpleCaseOrComparison extends POrComparison
{
    private PTerm _left_;
    private TOrComparator _orComparator_;
    private PTerm _right_;
    private final LinkedList<PRepeatingOrComparison> _moreRight_ = new LinkedList<PRepeatingOrComparison>();

    public ASimpleCaseOrComparison()
    {
        // Constructor
    }

    public ASimpleCaseOrComparison(
        @SuppressWarnings("hiding") PTerm _left_,
        @SuppressWarnings("hiding") TOrComparator _orComparator_,
        @SuppressWarnings("hiding") PTerm _right_,
        @SuppressWarnings("hiding") List<?> _moreRight_)
    {
        // Constructor
        setLeft(_left_);

        setOrComparator(_orComparator_);

        setRight(_right_);

        setMoreRight(_moreRight_);

    }

    @Override
    public Object clone()
    {
        return new ASimpleCaseOrComparison(
            cloneNode(this._left_),
            cloneNode(this._orComparator_),
            cloneNode(this._right_),
            cloneList(this._moreRight_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseASimpleCaseOrComparison(this);
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

    public TOrComparator getOrComparator()
    {
        return this._orComparator_;
    }

    public void setOrComparator(TOrComparator node)
    {
        if(this._orComparator_ != null)
        {
            this._orComparator_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._orComparator_ = node;
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

    public LinkedList<PRepeatingOrComparison> getMoreRight()
    {
        return this._moreRight_;
    }

    public void setMoreRight(List<?> list)
    {
        for(PRepeatingOrComparison e : this._moreRight_)
        {
            e.parent(null);
        }
        this._moreRight_.clear();

        for(Object obj_e : list)
        {
            PRepeatingOrComparison e = (PRepeatingOrComparison) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._moreRight_.add(e);
        }
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._left_)
            + toString(this._orComparator_)
            + toString(this._right_)
            + toString(this._moreRight_);
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

        if(this._orComparator_ == child)
        {
            this._orComparator_ = null;
            return;
        }

        if(this._right_ == child)
        {
            this._right_ = null;
            return;
        }

        if(this._moreRight_.remove(child))
        {
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

        if(this._orComparator_ == oldChild)
        {
            setOrComparator((TOrComparator) newChild);
            return;
        }

        if(this._right_ == oldChild)
        {
            setRight((PTerm) newChild);
            return;
        }

        for(ListIterator<PRepeatingOrComparison> i = this._moreRight_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PRepeatingOrComparison) newChild);
                    newChild.parent(this);
                    oldChild.parent(null);
                    return;
                }

                i.remove();
                oldChild.parent(null);
                return;
            }
        }

        throw new RuntimeException("Not a child.");
    }
}

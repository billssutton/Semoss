/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AJoinGroup extends PJoinGroup
{
    private TComma _comma_;
    private final LinkedList<PJoinParam> _joinParam_ = new LinkedList<PJoinParam>();

    public AJoinGroup()
    {
        // Constructor
    }

    public AJoinGroup(
        @SuppressWarnings("hiding") TComma _comma_,
        @SuppressWarnings("hiding") List<?> _joinParam_)
    {
        // Constructor
        setComma(_comma_);

        setJoinParam(_joinParam_);

    }

    @Override
    public Object clone()
    {
        return new AJoinGroup(
            cloneNode(this._comma_),
            cloneList(this._joinParam_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAJoinGroup(this);
    }

    public TComma getComma()
    {
        return this._comma_;
    }

    public void setComma(TComma node)
    {
        if(this._comma_ != null)
        {
            this._comma_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._comma_ = node;
    }

    public LinkedList<PJoinParam> getJoinParam()
    {
        return this._joinParam_;
    }

    public void setJoinParam(List<?> list)
    {
        for(PJoinParam e : this._joinParam_)
        {
            e.parent(null);
        }
        this._joinParam_.clear();

        for(Object obj_e : list)
        {
            PJoinParam e = (PJoinParam) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._joinParam_.add(e);
        }
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._comma_)
            + toString(this._joinParam_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._comma_ == child)
        {
            this._comma_ = null;
            return;
        }

        if(this._joinParam_.remove(child))
        {
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._comma_ == oldChild)
        {
            setComma((TComma) newChild);
            return;
        }

        for(ListIterator<PJoinParam> i = this._joinParam_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PJoinParam) newChild);
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

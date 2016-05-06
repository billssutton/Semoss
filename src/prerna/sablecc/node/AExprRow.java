/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import java.util.*;
import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AExprRow extends PExprRow
{
    private TLBracket _lBracket_;
    private PExpr _expr_;
    private final LinkedList<PExprGroup> _exprGroup_ = new LinkedList<PExprGroup>();
    private TRBracket _rBracket_;

    public AExprRow()
    {
        // Constructor
    }

    public AExprRow(
        @SuppressWarnings("hiding") TLBracket _lBracket_,
        @SuppressWarnings("hiding") PExpr _expr_,
        @SuppressWarnings("hiding") List<?> _exprGroup_,
        @SuppressWarnings("hiding") TRBracket _rBracket_)
    {
        // Constructor
        setLBracket(_lBracket_);

        setExpr(_expr_);

        setExprGroup(_exprGroup_);

        setRBracket(_rBracket_);

    }

    @Override
    public Object clone()
    {
        return new AExprRow(
            cloneNode(this._lBracket_),
            cloneNode(this._expr_),
            cloneList(this._exprGroup_),
            cloneNode(this._rBracket_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAExprRow(this);
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

    public PExpr getExpr()
    {
        return this._expr_;
    }

    public void setExpr(PExpr node)
    {
        if(this._expr_ != null)
        {
            this._expr_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._expr_ = node;
    }

    public LinkedList<PExprGroup> getExprGroup()
    {
        return this._exprGroup_;
    }

    public void setExprGroup(List<?> list)
    {
        for(PExprGroup e : this._exprGroup_)
        {
            e.parent(null);
        }
        this._exprGroup_.clear();

        for(Object obj_e : list)
        {
            PExprGroup e = (PExprGroup) obj_e;
            if(e.parent() != null)
            {
                e.parent().removeChild(e);
            }

            e.parent(this);
            this._exprGroup_.add(e);
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
            + toString(this._expr_)
            + toString(this._exprGroup_)
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

        if(this._expr_ == child)
        {
            this._expr_ = null;
            return;
        }

        if(this._exprGroup_.remove(child))
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

        if(this._expr_ == oldChild)
        {
            setExpr((PExpr) newChild);
            return;
        }

        for(ListIterator<PExprGroup> i = this._exprGroup_.listIterator(); i.hasNext();)
        {
            if(i.next() == oldChild)
            {
                if(newChild != null)
                {
                    i.set((PExprGroup) newChild);
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

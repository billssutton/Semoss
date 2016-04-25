/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AAliasColumn extends PAliasColumn
{
    private TColalias _colalias_;
    private TLPar _lp1_;
    private PColCsv _cols_;
    private PWhereStatement _where_;
    private TRPar _rp2_;

    public AAliasColumn()
    {
        // Constructor
    }

    public AAliasColumn(
        @SuppressWarnings("hiding") TColalias _colalias_,
        @SuppressWarnings("hiding") TLPar _lp1_,
        @SuppressWarnings("hiding") PColCsv _cols_,
        @SuppressWarnings("hiding") PWhereStatement _where_,
        @SuppressWarnings("hiding") TRPar _rp2_)
    {
        // Constructor
        setColalias(_colalias_);

        setLp1(_lp1_);

        setCols(_cols_);

        setWhere(_where_);

        setRp2(_rp2_);

    }

    @Override
    public Object clone()
    {
        return new AAliasColumn(
            cloneNode(this._colalias_),
            cloneNode(this._lp1_),
            cloneNode(this._cols_),
            cloneNode(this._where_),
            cloneNode(this._rp2_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAAliasColumn(this);
    }

    public TColalias getColalias()
    {
        return this._colalias_;
    }

    public void setColalias(TColalias node)
    {
        if(this._colalias_ != null)
        {
            this._colalias_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._colalias_ = node;
    }

    public TLPar getLp1()
    {
        return this._lp1_;
    }

    public void setLp1(TLPar node)
    {
        if(this._lp1_ != null)
        {
            this._lp1_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._lp1_ = node;
    }

    public PColCsv getCols()
    {
        return this._cols_;
    }

    public void setCols(PColCsv node)
    {
        if(this._cols_ != null)
        {
            this._cols_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._cols_ = node;
    }

    public PWhereStatement getWhere()
    {
        return this._where_;
    }

    public void setWhere(PWhereStatement node)
    {
        if(this._where_ != null)
        {
            this._where_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._where_ = node;
    }

    public TRPar getRp2()
    {
        return this._rp2_;
    }

    public void setRp2(TRPar node)
    {
        if(this._rp2_ != null)
        {
            this._rp2_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._rp2_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._colalias_)
            + toString(this._lp1_)
            + toString(this._cols_)
            + toString(this._where_)
            + toString(this._rp2_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._colalias_ == child)
        {
            this._colalias_ = null;
            return;
        }

        if(this._lp1_ == child)
        {
            this._lp1_ = null;
            return;
        }

        if(this._cols_ == child)
        {
            this._cols_ = null;
            return;
        }

        if(this._where_ == child)
        {
            this._where_ = null;
            return;
        }

        if(this._rp2_ == child)
        {
            this._rp2_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._colalias_ == oldChild)
        {
            setColalias((TColalias) newChild);
            return;
        }

        if(this._lp1_ == oldChild)
        {
            setLp1((TLPar) newChild);
            return;
        }

        if(this._cols_ == oldChild)
        {
            setCols((PColCsv) newChild);
            return;
        }

        if(this._where_ == oldChild)
        {
            setWhere((PWhereStatement) newChild);
            return;
        }

        if(this._rp2_ == oldChild)
        {
            setRp2((TRPar) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

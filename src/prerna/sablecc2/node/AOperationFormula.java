/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AOperationFormula extends POperationFormula
{
    private TId _id_;
    private PPlainRow _plainRow_;
    private PAsop _asop_;

    public AOperationFormula()
    {
        // Constructor
    }

    public AOperationFormula(
        @SuppressWarnings("hiding") TId _id_,
        @SuppressWarnings("hiding") PPlainRow _plainRow_,
        @SuppressWarnings("hiding") PAsop _asop_)
    {
        // Constructor
        setId(_id_);

        setPlainRow(_plainRow_);

        setAsop(_asop_);

    }

    @Override
    public Object clone()
    {
        return new AOperationFormula(
            cloneNode(this._id_),
            cloneNode(this._plainRow_),
            cloneNode(this._asop_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAOperationFormula(this);
    }

    public TId getId()
    {
        return this._id_;
    }

    public void setId(TId node)
    {
        if(this._id_ != null)
        {
            this._id_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._id_ = node;
    }

    public PPlainRow getPlainRow()
    {
        return this._plainRow_;
    }

    public void setPlainRow(PPlainRow node)
    {
        if(this._plainRow_ != null)
        {
            this._plainRow_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._plainRow_ = node;
    }

    public PAsop getAsop()
    {
        return this._asop_;
    }

    public void setAsop(PAsop node)
    {
        if(this._asop_ != null)
        {
            this._asop_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._asop_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._id_)
            + toString(this._plainRow_)
            + toString(this._asop_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._id_ == child)
        {
            this._id_ = null;
            return;
        }

        if(this._plainRow_ == child)
        {
            this._plainRow_ = null;
            return;
        }

        if(this._asop_ == child)
        {
            this._asop_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._id_ == oldChild)
        {
            setId((TId) newChild);
            return;
        }

        if(this._plainRow_ == oldChild)
        {
            setPlainRow((PPlainRow) newChild);
            return;
        }

        if(this._asop_ == oldChild)
        {
            setAsop((PAsop) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

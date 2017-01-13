/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AMoveFrame extends PMoveFrame
{
    private TMv _mv_;
    private TLPar _lPar_;
    private TId _from_;
    private TComma _c1_;
    private TId _to_;
    private PFrameType _frameType_;
    private TComma _c2_;
    private PSelectors _selectors_;
    private TRPar _rPar_;

    public AMoveFrame()
    {
        // Constructor
    }

    public AMoveFrame(
        @SuppressWarnings("hiding") TMv _mv_,
        @SuppressWarnings("hiding") TLPar _lPar_,
        @SuppressWarnings("hiding") TId _from_,
        @SuppressWarnings("hiding") TComma _c1_,
        @SuppressWarnings("hiding") TId _to_,
        @SuppressWarnings("hiding") PFrameType _frameType_,
        @SuppressWarnings("hiding") TComma _c2_,
        @SuppressWarnings("hiding") PSelectors _selectors_,
        @SuppressWarnings("hiding") TRPar _rPar_)
    {
        // Constructor
        setMv(_mv_);

        setLPar(_lPar_);

        setFrom(_from_);

        setC1(_c1_);

        setTo(_to_);

        setFrameType(_frameType_);

        setC2(_c2_);

        setSelectors(_selectors_);

        setRPar(_rPar_);

    }

    @Override
    public Object clone()
    {
        return new AMoveFrame(
            cloneNode(this._mv_),
            cloneNode(this._lPar_),
            cloneNode(this._from_),
            cloneNode(this._c1_),
            cloneNode(this._to_),
            cloneNode(this._frameType_),
            cloneNode(this._c2_),
            cloneNode(this._selectors_),
            cloneNode(this._rPar_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAMoveFrame(this);
    }

    public TMv getMv()
    {
        return this._mv_;
    }

    public void setMv(TMv node)
    {
        if(this._mv_ != null)
        {
            this._mv_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._mv_ = node;
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

    public TId getFrom()
    {
        return this._from_;
    }

    public void setFrom(TId node)
    {
        if(this._from_ != null)
        {
            this._from_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._from_ = node;
    }

    public TComma getC1()
    {
        return this._c1_;
    }

    public void setC1(TComma node)
    {
        if(this._c1_ != null)
        {
            this._c1_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._c1_ = node;
    }

    public TId getTo()
    {
        return this._to_;
    }

    public void setTo(TId node)
    {
        if(this._to_ != null)
        {
            this._to_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._to_ = node;
    }

    public PFrameType getFrameType()
    {
        return this._frameType_;
    }

    public void setFrameType(PFrameType node)
    {
        if(this._frameType_ != null)
        {
            this._frameType_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._frameType_ = node;
    }

    public TComma getC2()
    {
        return this._c2_;
    }

    public void setC2(TComma node)
    {
        if(this._c2_ != null)
        {
            this._c2_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._c2_ = node;
    }

    public PSelectors getSelectors()
    {
        return this._selectors_;
    }

    public void setSelectors(PSelectors node)
    {
        if(this._selectors_ != null)
        {
            this._selectors_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._selectors_ = node;
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
            + toString(this._mv_)
            + toString(this._lPar_)
            + toString(this._from_)
            + toString(this._c1_)
            + toString(this._to_)
            + toString(this._frameType_)
            + toString(this._c2_)
            + toString(this._selectors_)
            + toString(this._rPar_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._mv_ == child)
        {
            this._mv_ = null;
            return;
        }

        if(this._lPar_ == child)
        {
            this._lPar_ = null;
            return;
        }

        if(this._from_ == child)
        {
            this._from_ = null;
            return;
        }

        if(this._c1_ == child)
        {
            this._c1_ = null;
            return;
        }

        if(this._to_ == child)
        {
            this._to_ = null;
            return;
        }

        if(this._frameType_ == child)
        {
            this._frameType_ = null;
            return;
        }

        if(this._c2_ == child)
        {
            this._c2_ = null;
            return;
        }

        if(this._selectors_ == child)
        {
            this._selectors_ = null;
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
        if(this._mv_ == oldChild)
        {
            setMv((TMv) newChild);
            return;
        }

        if(this._lPar_ == oldChild)
        {
            setLPar((TLPar) newChild);
            return;
        }

        if(this._from_ == oldChild)
        {
            setFrom((TId) newChild);
            return;
        }

        if(this._c1_ == oldChild)
        {
            setC1((TComma) newChild);
            return;
        }

        if(this._to_ == oldChild)
        {
            setTo((TId) newChild);
            return;
        }

        if(this._frameType_ == oldChild)
        {
            setFrameType((PFrameType) newChild);
            return;
        }

        if(this._c2_ == oldChild)
        {
            setC2((TComma) newChild);
            return;
        }

        if(this._selectors_ == oldChild)
        {
            setSelectors((PSelectors) newChild);
            return;
        }

        if(this._rPar_ == oldChild)
        {
            setRPar((TRPar) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

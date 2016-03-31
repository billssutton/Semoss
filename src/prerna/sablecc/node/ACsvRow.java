/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ACsvRow extends PCsvRow
{
    private TLBracket _lBracket_;
    private PWordOrNum _wordOrNum_;
    private PCsvGroup _csvGroup_;
    private TRBracket _rBracket_;

    public ACsvRow()
    {
        // Constructor
    }

    public ACsvRow(
        @SuppressWarnings("hiding") TLBracket _lBracket_,
        @SuppressWarnings("hiding") PWordOrNum _wordOrNum_,
        @SuppressWarnings("hiding") PCsvGroup _csvGroup_,
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
            cloneNode(this._csvGroup_),
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

    public PCsvGroup getCsvGroup()
    {
        return this._csvGroup_;
    }

    public void setCsvGroup(PCsvGroup node)
    {
        if(this._csvGroup_ != null)
        {
            this._csvGroup_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._csvGroup_ = node;
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

        if(this._csvGroup_ == child)
        {
            this._csvGroup_ = null;
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

        if(this._csvGroup_ == oldChild)
        {
            setCsvGroup((PCsvGroup) newChild);
            return;
        }

        if(this._rBracket_ == oldChild)
        {
            setRBracket((TRBracket) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

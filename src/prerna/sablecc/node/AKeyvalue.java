/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class AKeyvalue extends PKeyvalue
{
    private PWordOrNum _word1_;
    private TColon _colon_;
    private PWordOrNumOrNestedObj _word2_;

    public AKeyvalue()
    {
        // Constructor
    }

    public AKeyvalue(
        @SuppressWarnings("hiding") PWordOrNum _word1_,
        @SuppressWarnings("hiding") TColon _colon_,
        @SuppressWarnings("hiding") PWordOrNumOrNestedObj _word2_)
    {
        // Constructor
        setWord1(_word1_);

        setColon(_colon_);

        setWord2(_word2_);

    }

    @Override
    public Object clone()
    {
        return new AKeyvalue(
            cloneNode(this._word1_),
            cloneNode(this._colon_),
            cloneNode(this._word2_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAKeyvalue(this);
    }

    public PWordOrNum getWord1()
    {
        return this._word1_;
    }

    public void setWord1(PWordOrNum node)
    {
        if(this._word1_ != null)
        {
            this._word1_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._word1_ = node;
    }

    public TColon getColon()
    {
        return this._colon_;
    }

    public void setColon(TColon node)
    {
        if(this._colon_ != null)
        {
            this._colon_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._colon_ = node;
    }

    public PWordOrNumOrNestedObj getWord2()
    {
        return this._word2_;
    }

    public void setWord2(PWordOrNumOrNestedObj node)
    {
        if(this._word2_ != null)
        {
            this._word2_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._word2_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._word1_)
            + toString(this._colon_)
            + toString(this._word2_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._word1_ == child)
        {
            this._word1_ = null;
            return;
        }

        if(this._colon_ == child)
        {
            this._colon_ = null;
            return;
        }

        if(this._word2_ == child)
        {
            this._word2_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._word1_ == oldChild)
        {
            setWord1((PWordOrNum) newChild);
            return;
        }

        if(this._colon_ == oldChild)
        {
            setColon((TColon) newChild);
            return;
        }

        if(this._word2_ == oldChild)
        {
            setWord2((PWordOrNumOrNestedObj) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

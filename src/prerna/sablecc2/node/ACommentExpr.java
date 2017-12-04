/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class ACommentExpr extends PExpr
{
    private TComment _comment_;

    public ACommentExpr()
    {
        // Constructor
    }

    public ACommentExpr(
        @SuppressWarnings("hiding") TComment _comment_)
    {
        // Constructor
        setComment(_comment_);

    }

    @Override
    public Object clone()
    {
        return new ACommentExpr(
            cloneNode(this._comment_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseACommentExpr(this);
    }

    public TComment getComment()
    {
        return this._comment_;
    }

    public void setComment(TComment node)
    {
        if(this._comment_ != null)
        {
            this._comment_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._comment_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._comment_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._comment_ == child)
        {
            this._comment_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._comment_ == oldChild)
        {
            setComment((TComment) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

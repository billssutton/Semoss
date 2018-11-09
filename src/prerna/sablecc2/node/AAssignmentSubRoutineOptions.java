/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class AAssignmentSubRoutineOptions extends PSubRoutineOptions
{
    private PBaseAssignment _baseAssignment_;

    public AAssignmentSubRoutineOptions()
    {
        // Constructor
    }

    public AAssignmentSubRoutineOptions(
        @SuppressWarnings("hiding") PBaseAssignment _baseAssignment_)
    {
        // Constructor
        setBaseAssignment(_baseAssignment_);

    }

    @Override
    public Object clone()
    {
        return new AAssignmentSubRoutineOptions(
            cloneNode(this._baseAssignment_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseAAssignmentSubRoutineOptions(this);
    }

    public PBaseAssignment getBaseAssignment()
    {
        return this._baseAssignment_;
    }

    public void setBaseAssignment(PBaseAssignment node)
    {
        if(this._baseAssignment_ != null)
        {
            this._baseAssignment_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._baseAssignment_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._baseAssignment_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._baseAssignment_ == child)
        {
            this._baseAssignment_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._baseAssignment_ == oldChild)
        {
            setBaseAssignment((PBaseAssignment) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

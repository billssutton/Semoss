/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class ADashboardAddDashboardop extends PDashboardop
{
    private PDashboardAdd _dashboardAdd_;

    public ADashboardAddDashboardop()
    {
        // Constructor
    }

    public ADashboardAddDashboardop(
        @SuppressWarnings("hiding") PDashboardAdd _dashboardAdd_)
    {
        // Constructor
        setDashboardAdd(_dashboardAdd_);

    }

    @Override
    public Object clone()
    {
        return new ADashboardAddDashboardop(
            cloneNode(this._dashboardAdd_));
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseADashboardAddDashboardop(this);
    }

    public PDashboardAdd getDashboardAdd()
    {
        return this._dashboardAdd_;
    }

    public void setDashboardAdd(PDashboardAdd node)
    {
        if(this._dashboardAdd_ != null)
        {
            this._dashboardAdd_.parent(null);
        }

        if(node != null)
        {
            if(node.parent() != null)
            {
                node.parent().removeChild(node);
            }

            node.parent(this);
        }

        this._dashboardAdd_ = node;
    }

    @Override
    public String toString()
    {
        return ""
            + toString(this._dashboardAdd_);
    }

    @Override
    void removeChild(@SuppressWarnings("unused") Node child)
    {
        // Remove child
        if(this._dashboardAdd_ == child)
        {
            this._dashboardAdd_ = null;
            return;
        }

        throw new RuntimeException("Not a child.");
    }

    @Override
    void replaceChild(@SuppressWarnings("unused") Node oldChild, @SuppressWarnings("unused") Node newChild)
    {
        // Replace child
        if(this._dashboardAdd_ == oldChild)
        {
            setDashboardAdd((PDashboardAdd) newChild);
            return;
        }

        throw new RuntimeException("Not a child.");
    }
}

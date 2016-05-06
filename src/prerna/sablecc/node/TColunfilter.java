/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class TColunfilter extends Token
{
    public TColunfilter()
    {
        super.setText("col.unfilter");
    }

    public TColunfilter(int line, int pos)
    {
        super.setText("col.unfilter");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TColunfilter(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTColunfilter(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TColunfilter text.");
    }
}

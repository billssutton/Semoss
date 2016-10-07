/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class TDataframeduplicates extends Token
{
    public TDataframeduplicates()
    {
        super.setText("data.frame.hasDuplicates");
    }

    public TDataframeduplicates(int line, int pos)
    {
        super.setText("data.frame.hasDuplicates");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TDataframeduplicates(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTDataframeduplicates(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TDataframeduplicates text.");
    }
}

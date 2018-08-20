/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package svo.extractor;

/**
 *
 * @author wbolduc
 */
public class Word {
    public final int index;
    public final String form;
    public final String pos;
    public int head;
    public final String depRel;

    public Word(String index, String form, String pos, String head, String depRel) {
        this.index = Integer.parseInt(index);
        this.form = form;
        this.pos = pos;
        this.head = Integer.parseInt(head);
        if ("null".equals(depRel))
            this.depRel = "root";
        else
            this.depRel = depRel;
    }
    
    public Word(int index, String form, String pos, int head, String depRel) {
        this.index = index;
        this.form = form;
        this.pos = pos;
        this.head = head;
        if ("null".equals(depRel))
            this.depRel = "root";
        else
            this.depRel = depRel;
    }
    
}

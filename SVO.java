/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package svo.extractor;

import java.util.Objects;

/**
 *
 * @author wbolduc
 */
public class SVO {
    private final String subject;
    private final String verb;
    private final String object;
    private final boolean negated;
    
    public SVO(String subject, String verb, String object, boolean negated) {
        this.subject = subject;
        this.verb = verb;
        this.object = object;
        this.negated = negated;
    }

    public String getSubject() {
        return subject;
    }

    public String getVerb() {
        return verb;
    }

    public String getObject() {
        return object;
    }

    public boolean isNegated() {
        return negated;
    }

    @Override
    public String toString() {
        if (negated)
            return "SVO{" + "subject=" + subject + ", verb= not " + verb + ", object=" + object + '}';
        else
            return "SVO{" + "subject=" + subject + ", verb=" + verb + ", object=" + object + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.subject);
        hash = 97 * hash + Objects.hashCode(this.verb);
        hash = 97 * hash + Objects.hashCode(this.object);
        hash = 97 * hash + (this.negated ? 1 : 0);
        return hash;
    }
    
    

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SVO other = (SVO) obj;
        if (this.negated != other.negated) {
            return false;
        }
        if (!Objects.equals(this.subject, other.subject)) {
            return false;
        }
        if (!Objects.equals(this.verb, other.verb)) {
            return false;
        }
        if (!Objects.equals(this.object, other.object)) {
            return false;
        }
        return true;
    }
    

    
}

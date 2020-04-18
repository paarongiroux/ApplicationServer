/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appserver.job.impl;
import appserver.job.Tool;

/**
 *
 * @author aarongiroux
 */
public class Fibonacci implements Tool{
    
    @Override
    public Object go(Object parameters) {
        
        return fibonacci((Integer) parameters);
    }
    
    
    public int fibonacci(Integer number)
    {
        if (number == 0)
        {
            return 0;
        }
        if (number == 1)
        {
            return 1;
        }
        return fibonacci(number-1) + fibonacci(number-2);
    }
}

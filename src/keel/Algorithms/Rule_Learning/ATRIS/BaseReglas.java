/**
 * *********************************************************************
 *
 * This file is part of KEEL-software, the Data Mining tool for regression,
 * classification, clustering, pattern mining and so on.
 *
 * Copyright (C) 2004-2010
 *
 * F. Herrera (herrera@decsai.ugr.es) L. S�nchez (luciano@uniovi.es) J.
 * Alcal�-Fdez (jalcala@decsai.ugr.es) S. Garc�a (sglopez@ujaen.es) A. Fern�ndez
 * (alberto.fernandez@ujaen.es) J. Luengo (julianlm@decsai.ugr.es)
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/
 *
 *********************************************************************
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package keel.Algorithms.Rule_Learning.ATRIS;

import java.io.*;
import java.util.*;
import keel.Dataset.Attribute;
import keel.Dataset.Attributes;

/**
 *
 * @author IDG
 */
public class BaseReglas {

    private LinkedList<String> base_de_reglas_salida = new LinkedList<String>();
    private LinkedList<LinkedList<LinkedList<Double>>> base_de_reglas = new LinkedList<LinkedList<LinkedList<Double>>>();
    private myDataset train;
    private ArrayList<int[]> reglasResult;

    public BaseReglas(ArrayList<int[]> array, ArrayList<Integer> clases, ArrayList<Integer> conjuntos, myDataset atrain) {

        double[] fila; //variable para almacenar los valores de una fila completa
        LinkedList<LinkedList<Double>> antecedentes = new LinkedList<LinkedList<Double>>();
        LinkedList<Double> arrayInt = new LinkedList<Double>();
        train = atrain;
        reglasResult = array;

        int r_aux = 0;
        double v_aux;
        
        Attribute at[] = Attributes.getOutputAttributes();
        
        for (int j = 0; j < array.size(); j++) { //para cada fila

            antecedentes.clear();
            int offset = 0;

            int i = 0;
            while (i < train.getnInputs()) { //mientras que en el conjunto haya más de 1, y el índice i sea válido                  
                arrayInt.clear();
                for (int s = 0; s < conjuntos.get(i); s++) {
                    int total = offset + s;

                    if (array.get(j)[total] == 1) {
                        arrayInt.add((double) s);
                    }
                }
                antecedentes.add(new LinkedList<Double>(arrayInt));

                offset += conjuntos.get(i);
                i++;

            }//Fin del while

            base_de_reglas.add((LinkedList<LinkedList<Double>>) antecedentes.clone());
            System.out.println("INDICE CLASES: " +  clases.get(j));
            
            base_de_reglas_salida.add(at[0].getNominalValue(clases.get(j)));

        } //Fin del for

    }

    private boolean valido(TreeMap<Integer, Double> antecedentes) {
        boolean valido = true;
        boolean coincidente;
        int output = -1;

        for (int i = 0; i < train.getnData(); i++) {
            coincidente = true;

            Iterator j = antecedentes.keySet().iterator();
            while (j.hasNext()) {
                int aux = (Integer) j.next();

                //Comparamos el valor de la Fila i-columna R[j] con el valor de R[j] que tenemos
                if (train.getExample(i)[aux] != antecedentes.get(aux)) {
                    coincidente = false;
                }
            }
            if (coincidente) { //Estamos en una fila que coincide con los valores de los inputs que tenemos en R
                if (output == -1) { //Si es la primera vez que es coincidente
                    output = train.getOutputAsInteger(i); //guardamos la salida
                } else if (output != train.getOutputAsInteger(i)) { //Si no, comparamos la salida de la fila actual con la anterior guardada
                    valido = false;                          //si no coincide, no es valido                    
                }
            }

        }

        return valido;
    }

    public LinkedList<String> compruebaReglas(myDataset test) {
        LinkedList<String> salida = new LinkedList<String>();
         Attribute at[] = Attributes.getOutputAttributes();

        //Para cada uno de lo ejemplo del test, comprobamos si lo cubre alguna regla
        for (int i = 0; i < test.size(); i++) {
            double[] ejemplo = test.getX()[i];
            boolean pertenece = false;

            for (int k = 0; k < base_de_reglas.size(); k++) { // Para cada una de la reglas
                for (int l = 0; l < base_de_reglas.get(k).size(); l++) {
                    pertenece = base_de_reglas.get(k).get(l).contains(ejemplo[l]);

                    if (!pertenece) { //Sino pertenece nos salimos del buble
                        break;
                    }
                }
                if (pertenece) {
                    System.out.println("El ejemplo " + i + "lo cubre la regla " + k + "con clase: " + base_de_reglas_salida.get(k));
                    salida.add(base_de_reglas_salida.get(k));
                    break;
                }
            }

            if (!pertenece) {
                System.out.println("El ejemplo" + i + " pertenece a la clase \"" + classByDefault() +"\" ");
                salida.add(classByDefault());
            }
        }
        
        return salida;
    }
    
    private String classByDefault(){
        Attribute at[] = Attributes.getOutputAttributes();
        Vector<String> clases = at[0].getNominalValuesList();
        
        for(int i = 0; i < clases.size(); i++){
            if(!base_de_reglas_salida.contains(clases.get(i))){
                return clases.get(i);
            }
        }
        return null;
    }

    public void mostrarReglas() {

        Attribute a[] = Attributes.getInputAttributes();
        Attribute s[] = Attributes.getOutputAttributes();
        String output = new String("");

        output += "BASE DE REGLAS: \n\n";
        //Numero de reglas
        output += "Número de reglas: " + base_de_reglas.size() + " \n\n";
        //Tamaño medio de las reglas obtenidas
        Double media_reglas = 0.0;
        for (int i = 0; i < base_de_reglas.size(); i++) {
            Integer aux = 0;
            for (int sa = 0; sa < base_de_reglas.get(i).size(); sa++) {
                aux += base_de_reglas.get(i).get(sa).size();

            }
            media_reglas += aux.doubleValue();
        }
        output += "Tamaño medio de las reglas obtenidas: " + media_reglas / base_de_reglas.size() + " \n\n";

        for (int i = 0; i < base_de_reglas.size(); i++) {

            //Iterator j = base_de_reglas.get(i).keySet().iterator();
            int j = 0;
            while (j < base_de_reglas.get(i).size()) {
                int l = 0;
                while (l < base_de_reglas.get(i).get(j).size()) {
                    output += "(" + a[j].getName() + ",";//almacena atributo

                    Integer valor = (base_de_reglas.get(i).get(j).get(l)).intValue();
                    String prueba = a[j].getNominalValue(valor);
                    //almacena valor atributo, si prueba==null, guarda el entero, sino la cadena
                    if (prueba == null) {
                        output += valor.toString() + ")";
                    } else {
                        output += prueba + ")";
                    }
                    l++;
                    if (l < base_de_reglas.get(i).get(j).size()) {
                        output += " & ";
                    }
                }
                j++;
            }
            output += " -> (" + s[0].getName()
                    + "," + base_de_reglas_salida.get(i) + ") \n";
            output += "------------------------------------\n";
        }
        System.out.println(output);

    }

    public void ficheroReglas(String ficheroReglas, String output) {

        //Mostramos la base de reglas:
        Attribute a[] = Attributes.getInputAttributes();
        Attribute s[] = Attributes.getOutputAttributes();

        try {
            FileOutputStream f = new FileOutputStream(ficheroReglas);
            DataOutputStream fis = new DataOutputStream((OutputStream) f);
            output += "BASE DE REGLAS: \n\n";
            //Numero de reglas
            output += "Número de reglas: " + base_de_reglas.size() + " \n\n";
            //Tamaño medio de las reglas obtenidas
            Double media_reglas = 0.0;
            for (int i = 0; i < base_de_reglas.size(); i++) {
                Integer aux = 0;
                for (int sa = 0; sa < base_de_reglas.get(i).size(); sa++) {
                    aux += base_de_reglas.get(i).get(sa).size();

                }
                System.out.println("Valor sumado: " + aux.doubleValue());
                media_reglas += aux.doubleValue();
            }
            output += "Tamaño medio de las reglas obtenidas: " + media_reglas / base_de_reglas.size() + " \n\n";

            for (int i = 0; i < base_de_reglas.size(); i++) {
                int j = 0;
                output += " [ ";
                
                while (j < base_de_reglas.get(i).size()) {
                    int l = 0;
                    while (l < base_de_reglas.get(i).get(j).size()) {
                        System.out.println("L es: " + l + " Tamano es: " + base_de_reglas.get(i).get(j).size());
                        output += "(" + a[j].getName() + ",";//almacena atributo

                        Integer valor = (base_de_reglas.get(i).get(j).get(l)).intValue();
                        String prueba = a[j].getNominalValue(valor);
                        //almacena valor atributo, si prueba==null, guarda el entero, sino la cadena
                        if (prueba == null) {
                            output += valor.toString() + ")";
                        } else {
                            output += prueba + ")";
                        }
                        l++;
                        if (l < base_de_reglas.get(i).get(j).size()) {
                            output += " | ";
                        }
                    }
                    if (j < base_de_reglas.get(i).size() - 1) {
                        output += " ] ";
                        output += " & ";
                        output += " [ ";
                    }
                    j++;
                }
                output += " ] ";
                output += " -> (" + s[0].getName()
                        + "," + base_de_reglas_salida.get(i) + ") \n";
                output += "------------------------------------\n";
            }
            System.out.println(output);
            fis.writeBytes(output);
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

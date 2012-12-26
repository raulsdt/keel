/**
 * @class Algorithm.java
 * @author Raúl Salazar de Torres
 * @author José Manuel Serrano Mármol 
 * @date 15/12/2012
 * @version 1.0
 * Asignatura de Ingeniería del Conocimiento - Universidad de Jaén
 */

package keel.Algorithms.Rule_Learning.ATRIS;

import java.io.IOException;
import java.util.*;
import org.core.Fichero;
import org.core.Randomize;


public class Algorithm {
    
    /** train: dataset del conjunto de ejemplos dedicado a entrenamiento
        val: dataset dedicado a validación y mejora del sistema
        test: dataset del conjunto de ejemplos dedicado al ejemplo del sistema*/
    myDataset train, val, test;
    
    /** outputTr: Cadena con directorio y nombre del fichero de training
     *  outputTst: Cadena con direcotorio y nombre del fichero de Test
     *  ouputReglas: Cadena con directorio y nombre del fichero donde se almacenan las reglas
     */
    String outputTr, outputTst, outputReglas;
    
    /** Tamaño del vector con el que se define la regla binaría en el sistema*/
    int sizeVectorRule = 0;
    
    /** Array que contiene el tamaño de cada una de las variables del problema */
    ArrayList<Integer> valoresAtributo = new ArrayList<Integer>();
    
    /** Reglas disponibles = 1 y no disponibles = 0 en el sistema */
    int[] arrayReglas; 
    
    /**  Clases disponible = 1 y no disponibles = 0 en el sistema*/
    int[] arrayClases; 
    
    /** Reglas existente para cada una de las reglas */
    LinkedList<LinkedList<Integer>> arrayofClass = new LinkedList<LinkedList<Integer>>();
    
    /** Reglas activas que existen para una regla dada */
    ArrayList<Integer> rulesActivated = new ArrayList<Integer>();
    
    /**  */
    private boolean somethingWrong = false; //to check if everything is correct.
    
    /** Array con las posibles combinaciones de vecinos que existen */
    ArrayList<String> vectorKnn = new ArrayList<String>();
    
    /** Cadenas necesarias para almacenamiento del mejor vecino  */
    String[] vecinos, vecinoMejor;
    
    /** Clase activa actualmente  */
    int nowfilm = 0;
    
    /**
     * Default constructor
     */
    public Algorithm() {
        
    }

    /**
     * Número de reglas activadas para la clase actual
     * @return Entero correspondiente al número de reglas activas.
     */
    private int rulesClassesActivated() {

        return rulesActivated.size();

    }

    /**
     * Constructor con parametros.
     * @param parameters Parametros parseados para la lectura de ficheros
     */
    public Algorithm(parseParameters parameters) {

        train = new myDataset();
        val = new myDataset();
        test = new myDataset();
        try {
            System.out.println("\nReading the training set: "
                    + parameters.getTrainingInputFile());
            train.readClassificationSet(parameters.getTrainingInputFile(), true);
            System.out.println("\nReading the validation set: " +
                               parameters.getValidationInputFile());
            val.readClassificationSet(parameters.getValidationInputFile(), false);
            System.out.println("\nReading the test set: "
                    + parameters.getTestInputFile());
            test.readClassificationSet(parameters.getTestInputFile(), false);
            arrayClases = new int[train.getnClasses()];
            arrayReglas = new int[train.getnData()];
        } catch (IOException e) {
            System.err.println(
                    "There was a problem while reading the input data-sets: "
                    + e);
            somethingWrong = true;
        }

        //We may check if there are some numerical attributes, because our algorithm may not handle them:
        somethingWrong = somethingWrong || train.hasNumericalAttributes();
        somethingWrong = somethingWrong || train.hasMissingAttributes();

        outputTr = parameters.getTrainingOutputFile();
        outputTst = parameters.getTestOutputFile();
        outputReglas = parameters.getReglasOutputFile();
    }

    /**
     * Devuelve el conjunto de reglas cubiertas tanto positivamente (posición 0 del vector de vuelta)
     * como el conjunto de reglas cubiertas negativamente (posición 1 del vector de retorno)
     * @param rule Regla la cual se quiere observar si es cubierta o no.
     * @param numberRule Regla inicial a partir de la cual se obtiene la regla a comprobar.
     * @return [0] Número de ejemplos cubiertas positivamente. [1] Número de ejemplos cubiertos negativamente
     */
    private int[] containCoverRules(int[] rule, int numberRule) {
        int[] vectorNumerico = new int[train.getnInputs()];
        boolean entra = true;
        int offset = 0;
        
        int[] totalCoversRules = new int[2];
        totalCoversRules[0] = 0; //Covers rules positivement
        totalCoversRules[1] = 0; //Covers rules negativement
        
        for (int i = 0; i < arrayReglas.length; i++) {
            entra = true;
            offset = 0;
            if (arrayReglas[i] == 1) { //Rule is Activated?
                for (int j = 0; j < train.getnInputs(); j++) {
                    if (rule[offset + (int) train.getX()[i][j]] != 1) {
                        entra = false;
                        offset +=valoresAtributo.get(j);
                        break;//Out of for
                    }
                    offset +=valoresAtributo.get(j);
                }
                if (entra) { // Is positivement or not?
                    if (train.getOutputAsInteger(i) == train.getOutputAsInteger(numberRule)) {
                        totalCoversRules[0]++;
                    }else{
                        totalCoversRules[1]++;
                    }
                }
            }
        }
        return totalCoversRules;
    }

    

    /**
     * Número de reglas existentes en la clase que nos encontremos, tantos activas como no.
     * @return Entero con el número de reglas de la clase
     */
    private int positiveRules() {

        return arrayofClass.get(nowfilm).size();
    }

    /**
     * Número de reglas de cualquier clase que se encuentran activas.
     * @return Entero con el número de reglas de cualquier clase que se encuentran activas.
     */
    private int numRuleActivate() {
        int suma = 0;
        for (int i = 0; i < arrayReglas.length; i++) {
            if (arrayReglas[i] == 1) {
                suma++;
            }
        }
        return suma;
    }

    /**
     * M-estimate que permite medir la calidad de la regla.
     * @param rule Regla de la cual se quiere obtener su nivel de calidad
     * @param numberRule Número de regla de partida a partir del cual se genera la regla derivada que se 
     * pasa como parametro.
     * @return Double como medida de calidad de la regla
     * @throws IOException 
     */
    private double mestimate(int[] rule, int numberRule) throws IOException {
        int P = positiveRules();
        int N = train.getX().length - P;
        int m = 2;
        int[] coversRules = containCoverRules(rule, numberRule);
        int p = coversRules[0];
        int n = coversRules[1];
        return (double) ((double) p + (double) m * (double) ((double) P / (double) (P + N))) / (double) (p + n + m);


        // TODO: ATRIS - Revisar el estimador.
    }

    /**
     * Conjunto de reglas activas para una clase dada
     * @param rules Reglas perteneciente a la clase en la que nos encontramos
     * @param arrayReglas Conjunto de reglas total (Muestra actividad de las reglas)
     * @return Array con el conjunto de reglas de conjunto rules que se encuentran activas,en función de arrayReglas. 
     */
    private ArrayList<Integer> rulesActivate(LinkedList<Integer> rules, int[] arrayReglas) {
        ArrayList<Integer> lista = new ArrayList<Integer>();
        for (int i = 0; i < rules.size(); i++) {
            if (arrayReglas[rules.get(i)] == 1) {
                lista.add(rules.get(i));
            }
        }
        return lista;
    }

    /**
     * Calculo del knn
     * @param cant Cantidad del kNN (Se usa 2-opt por defecto)
     * @return Devuelve una lista de Cadenas con las posibles combinaciones entre vecinos
     */
    private List<String> obtencionKnn(int cant) {

        int j = 0;
        String ci, cj;
        ArrayList<String> lista = new ArrayList<String>();
        for (int i = 0; i < cant; i++) {
            j = i + 1;
            while (j < cant) {
                ci = String.valueOf((int) i);
                cj = String.valueOf((int) j);
                ci = ci + "," + cj;
                lista.add(new String(ci));
                j++;
            }
        }
        return lista;
    }

    /**
     * Calculo del 2op (Genera el flit en los bits que sea oportuno)
     * @param vectorSalida Vector en el cual se realiza 2Opt
     * @return Array de enteros con el cambio en sus bits ya realizado.
     */
    private int[] _2opt(int[] vectorSalida) {

        Randomize rnd = new Randomize();
        int valoraleat = Randomize.Randint(0, vectorKnn.size());
        String v = vectorKnn.get(valoraleat);
        vectorKnn.remove(valoraleat);

        vecinos = v.split(",");

        if (vectorSalida[Integer.valueOf(vecinos[0])] == 1) {
            vectorSalida[Integer.valueOf(vecinos[0])] = 0;
        } else {
            vectorSalida[Integer.valueOf(vecinos[0])] = 1;
        }

        if (vectorSalida[Integer.valueOf(vecinos[1])] == 1) {
            vectorSalida[Integer.valueOf(vecinos[1])] = 0;
        } else {
            vectorSalida[Integer.valueOf(vecinos[1])] = 1;
        }


        return vectorSalida;
    }

    /**
     * Comprobador para eliminar aquellas reglas que tiene toda una variable llena de 0's
     * @param rule Regla a comprobar
     * @return True si la regla es validad y False en cualquier otro caso
     */
    private boolean comprobarSiReglaValida(int[] rule) {
        boolean correcto = false;
        int offset = 0; // 
        for (int i = 0; i < train.getnInputs(); i++) {
            correcto = false;
            for (int j = 0; j < valoresAtributo.get(i); j++) {
                if (rule[offset + j] == 1) {
                    correcto = true;
                }
            }
            offset += valoresAtributo.get(i);
            if (!correcto) {
                return false;
            }

        }
        return true;
    }

    /**
     * Proporciona la mejor regla para un ejemplo
     * @param example Número de ejemplo del cual se quiere obtener la mejor regla
     * @return Devuelve un array con la mejor regla
     * @throws IOException 
     */
    public int[] obtainBestRule(int example) throws IOException {
        
        //Vector con el valor de los diferentes ejemplos
        double[][] arrayValores = train.getX(); 
        //Campos doubles para mantener el resultado siempre de la mejor regla obtenida hasta el momento.
        double bestError = 0, resultado = 0;
        //Flip para comprobar si se trata de una nueva regla
        boolean nuevaRegla = true, acabado = false;



        int[] binaryRule = new int[sizeVectorRule];
        if (nuevaRegla) {
            int[] arrayOut = train.getOutputAsInteger();
            Arrays.sort(arrayOut);
            //sizeVectorRule +=arrayOut[arrayOut.length-1]+1; //Le sumamos la parte de la clase

            //Desde aqui cada nueva regla
            //Define the vector of the  - Inicializamos todo a cero
            for (int j = 0; j < sizeVectorRule; j++) {
                binaryRule[j] = 0;
            }


            //Conversion the Rule - Colocamos valores de uno donde sea necesario.
            int offset = 0;
            //binaryRule[(int) arrayValores[example][0]] = 1;
            for (int j = 0; j < train.getnInputs(); j++) {       
                binaryRule[offset + (int) arrayValores[example][j]] = 1;   
                offset += valoresAtributo.get(j);
            }
//            System.out.println("La regla inicial es : " + example);
//            for(int i=0; i< binaryRule.length;i++){
//                System.out.print(binaryRule[i]);
//            }
        }

        while (acabado != true) {
            //Calculamos el kNN
            vectorKnn = new ArrayList<String>(obtencionKnn(sizeVectorRule));

            //Calculate the 2-opt
            int i = 0;
            //System.out.println("Valor de sizeVectorRuele: " + sizeVectorRule);
            
            while (i < (sizeVectorRule * (sizeVectorRule - 1) / 2)) {
                int[] ruleVecina = new int[sizeVectorRule];
                boolean reglaValida = false;
                while (!reglaValida && i < (sizeVectorRule * (sizeVectorRule - 1) / 2)) {
                    ruleVecina = _2opt(binaryRule);
                    reglaValida = comprobarSiReglaValida(ruleVecina);
                    if(!reglaValida){
                        i++;
                    }
                }
                
                resultado = mestimate(ruleVecina, example);
                
                if (bestError < resultado) {// Si entra en su interior es porque el resultado es mejor que el anterior. (Más alto)
                    bestError = resultado;
                    vecinoMejor = vecinos;
                    i = 0;
                    vectorKnn = new ArrayList<String>(obtencionKnn(sizeVectorRule));
                    binaryRule = ruleVecina;
                    nuevaRegla = false;
                    break;
                }

                if (i == (sizeVectorRule * (sizeVectorRule - 1) / 2) - 1) {
                    acabado = true;
                    break;
                }
                i++;

            }


        }


        //Comentario para mostrar datos por pantalla
//        System.out.println("Ejemplo: " + example);
//
//        System.out.println("Regla: ");
//        for (int i = 0; i < sizeVectorRule; i++) {
//            if(i!= sizeVectorRule - 1)
//                System.out.print(binaryRule[i] + ", ");
//            else
//                System.out.print(binaryRule[i]);
//        }



        return binaryRule;
    }

    /**
     * Metodo de ejecución del sistema en completo
     * @throws IOException 
     */
    public void execute() throws IOException {

        ArrayList<int[]> conjBestRuleObtained = new ArrayList<int[]>();
        ArrayList<Integer> classBestRuleObtained = new ArrayList<Integer>();
        int[] bestRuleObtained;
        Random rnd = new Random(234);
        int[] valores = new int[train.getOutputAsInteger().length];
        

        if (somethingWrong) { //We do not execute the program
            System.err.println("An error was found, either the data-set have numerical values or missing values.");
            System.err.println("Aborting the program");
            //We should not use the statement: System.exit(-1);
        } else {
            //Comprobaciones a realizar 
            if (train.hasNumericalAttributes()) {
                System.out.println("Se trata de atributos numericos");
                //TODO: Tendriamos que devolver fallo?
            } else if (train.hasRealAttributes()) {
                System.out.println("Se trata de atributos reales");
                //TODO: Tendriamos que devolver fallo?
            } else {
                System.out.println("Se trata de otros tipos de atributo");
            }


            //Determination vector of different Rules
            for (int i = 0; i < train.getnClasses(); i++) {
                arrayClases[i] = 1;
            }

            for (int i = 0; i < train.getnData(); i++) {
                arrayReglas[i] = 1;
            }

            //Organized each out with her class
            for (int i = 0; i < train.getnClasses(); i++) {
                arrayofClass.add(new LinkedList<Integer>());
            }

            for (int i = 0; i < train.getOutputAsInteger().length; i++) {
                arrayofClass.get(train.getOutputAsInteger(i)).add(i);
            }

            //Preparate for transladation to Binary number - Definition tam of binary vector
            for(int i=0;i < train.devuelveRangos().length-1;i++){
                sizeVectorRule += (int)train.devuelveRangos()[i][1]+1;
                valoresAtributo.add((int)train.devuelveRangos()[i][1]+1);
            }
            
            int numReglasAnterior=0;
            
            int reglasActivasParaClase = 0;

            for (int nClases = 0; nClases < train.getnClasses() - 1; nClases++) {
                //Obtain the number of aleatory rule 
                nowfilm = selectClass();
                reglasActivasParaClase = arrayofClass.get(nowfilm).size();
                while (reglasActivasParaClase != 0) {
                    rulesActivated = rulesActivate(arrayofClass.get(nowfilm), arrayReglas);
                    System.out.println("Reglas activas: " + rulesActivated.size());
                    //Obtain de best rule for that example

                    bestRuleObtained = obtainBestRule(rulesActivated.get(rnd.nextInt(rulesActivated.size())));
                    
                    reglasActivasParaClase = deleteRuleCovert(nowfilm, bestRuleObtained);
                    if(numReglasAnterior != reglasActivasParaClase){
                        conjBestRuleObtained.add(bestRuleObtained);
                        classBestRuleObtained.add(nowfilm);
                    }
                    numReglasAnterior = reglasActivasParaClase;
                    //System.out.println("Quedan reglas para clase: " + reglasActivasParaClase);
                    imprimirReglaGenerada(bestRuleObtained, nowfilm);
                    
                    
                    
                    //TODO: ATRIS - Eliminar aquellos ejemplos cubiertos por la mejor regla obtenida anteriormente.
                }
                arrayClases[nowfilm] = 0;
            }
            
            //###################Inducimos la base de reglas####################
            String output = new String("");
            BaseReglas br = new BaseReglas(conjBestRuleObtained,classBestRuleObtained, valoresAtributo, train);
            br.ficheroReglas(outputReglas,output);
            
            //###################Comprobamos con el fichero de test#############
            LinkedList<String> resultado_test = br.compruebaReglas(test);
            
            //###################Comprobamos con el fochero de test#############
            LinkedList<String> resultado_val = br.compruebaReglas(val);
            
            doOutput(this.val, this.outputTr, resultado_val);
            doOutput(this.test, this.outputTst, resultado_test);

            System.out.println("Algorithm Finished");
        }
    }

    /**
     * Impresión de reglas
     * @param rule Array con el conjunto de reglas a imprimir
     * @param clase Clase en la cual nos encontramos actualmente
     */
    private void imprimirReglaGenerada(int[] rule, int clase) {
        System.out.println("");
        for (int i = 0; i < rule.length; i++) {
            if(i!= rule.length-1) {
                System.out.print(rule[i] + ", ");
            }
            else {
                System.out.print(rule[i]);
            }
        }
        System.out.print(" - Clase: " + clase);
    }

    /**
     * Método destinado a la eliminación de aquellas reglas que se cubren por el sistema.
     * @param clase Clase en la que nos encontramos actualmente
     * @param rule Mejor regla generada y a partir de la cual se quieren cubir otras
     * @return Número de reglas que quedan activas en la clase actual
     */
    private int deleteRuleCovert(int clase, int[] rule) {
        double[][] arrayValores = train.getX();
        boolean salir = false;
        int cantidadClase = 0;
        int offset = 0;

        for (int i = 0; i < arrayReglas.length; i++) {
//            System.out.println("Valor: ");
//            for(int j=0;j< train.getnInputs();j++){
//                System.out.print( arrayValores[i][j]);
//            }
            offset = 0;
            for (int k = 0; k < train.getnInputs(); k++) {
                if (rule[offset + (int) arrayValores[i][k]] != 1) {
                    salir = true;
                }
                offset += valoresAtributo.get(k);
            }
            if (!salir) {
                arrayReglas[i] = 0; //Desactivamos la regla
            } else {
                salir = false;
            }

        }
        return rulesActivate(arrayofClass.get(nowfilm), arrayReglas).size();
    }

   /**
    * Selección de clase con menos número de elemento
    * @return Número de la clase
    */
    private int selectClass() {

        int[] array = new int[train.getnClasses()];
        int countLess = 0, numberLess = 9999999;

        //Inicialize the whole of the class
        for (int i = 0; i < train.getnClasses(); i++) {
            array[i] = 0;
        }

        for (int i = 0; i < train.getOutputAsInteger().length; i++) {
            array[train.getOutputAsInteger(i)]++;
        }

        for (int i = 0; i < array.length; i++) {

            if (array[i] < numberLess && arrayClases[i] == 1) {
                countLess = i;
                numberLess = array[i];
            }

        }

        return countLess; //Return the number of class with less number of examples.
    }

    /**
     * Impresión del fichero de salida
     * @param dataset Conjunto de datos que se quieren imprimir
     * @param filename Ruta y nombre del fichero a crear.
     * @param resultado Texto a imprimir en el fichero
     */
    private void doOutput(myDataset dataset, String filename, LinkedList<String> resultado) {
        String output = new String("");
        output = dataset.copyHeader(); //we insert the header in the output file
        Double noacertados = 0.0;
        Double noclasificados = 0.0;
        //We write the output for each example
        for (int i = 0; i < dataset.getnData(); i++) {
            //for classification:
            output += dataset.getOutputAsString(i) + " "
                    + resultado.get(i) + "\n";

            if (resultado.get(i).compareTo("No clasificado") == 0) {
                noclasificados++;
            } else if (dataset.getOutputAsString(i).compareTo(resultado.get(i)) != 0) {
                noacertados++;
            }
        }
        Fichero.escribeFichero(filename, output);
    }
}

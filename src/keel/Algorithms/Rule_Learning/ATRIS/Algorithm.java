/**
 * @author Raúl Salazar de Torres
 * @author José Manuel Serrano Mármol @date 15/12/2012
 * @version 1.0
 */
package keel.Algorithms.Rule_Learning.ATRIS;

/**
 * <p>Title: Algorithm</p>
 *
 * <p>Description: It contains the implementation of the algorithm</p>
 *
 *
 * <p>Company: KEEL </p>
 *
 * @author Alberto Fernandez
 * @version 1.0
 */
import com.lowagie.text.pdf.ArabicLigaturizer;
import java.io.IOException;
import org.core.*;
import java.util.*;
import keel.Dataset.Attribute;
import keel.Dataset.Attributes;

public class Algorithm {

    myDataset train, val, test;
    String outputTr, outputTst, outputReglas;
    int sizeVectorRule = 0;
    ArrayList<Integer> valoresAtributo = new ArrayList<Integer>();
    int[] arrayReglas; //Reglas disponibles
    int[] arrayClases; //Clases disponibles
    LinkedList<LinkedList<Integer>> arrayofClass = new LinkedList<LinkedList<Integer>>();
    ArrayList<Integer> rulesActivated = new ArrayList<Integer>();
    private boolean somethingWrong = false; //to check if everything is correct.
    ArrayList<String> vectorKnn = new ArrayList<String>();
    String[] vecinos, vecinoMejor;
    int nowfilm = 0;

    /**
     * Default constructor
     */
    public Algorithm() {
    }

    /**
     * Methode to know the number of activated rules for active class
     *
     * @return number of activated rules
     */
    private int rulesClassesActivated() {

        return rulesActivated.size();

    }

    /**
     * It reads the data from the input files (training, validation and test)
     * and parse all the parameters from the parameters array.
     *
     * @param parameters parseParameters It contains the input files, output
     * files and parameters
     */
    public Algorithm(parseParameters parameters) {

        train = new myDataset();
        val = new myDataset();
        test = new myDataset();
        try {
            System.out.println("\nReading the training set: "
                    + parameters.getTrainingInputFile());
            train.readClassificationSet(parameters.getTrainingInputFile(), true);
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
     * Number of rules that cover to a rule positivement
     *
     * @param rule
     * @param numberRule
     * @return
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
                        break;//Comprobar
                    }
                    offset +=valoresAtributo.get(j);
                }
                if (entra) { 
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
     * Num or rules that are cover for it
     *
     * @param rule
     * @return
     */
    private int positiveRules() {

        return arrayofClass.get(nowfilm).size();
    }

    /**
     * Whole of rule active
     *
     * @return
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
     * Calculate the mestimate
     *
     * @param rule
     * @param numberRule
     * @return
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
     * Obtain de rules that there is activated nowdays
     *
     * @param rules
     * @param arrayReglas
     * @return
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
     * Calculate combination of knn
     *
     * @param cant
     * @return
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
     * Calcule combination 2opt
     *
     * @return
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
     * Obtain the best rule about a determinate rule (example)
     *
     * @param example
     * @return
     * @throws IOException
     */
    public int[] obtainBestRule(int example) throws IOException {

        double[][] arrayValores = train.getX();
        double bestError = 0, resultado = 0;
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
                while (!reglaValida) {
                    ruleVecina = _2opt(binaryRule);
                    reglaValida = comprobarSiReglaValida(ruleVecina);
                    if(!reglaValida){
                        i++;
                    }
                }

                resultado = mestimate(ruleVecina, example);
                
                if (bestError < resultado) {
                    bestError = resultado;
                    vecinoMejor = vecinos;
                    //i = 0;
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
     * It launches the algorithm
     */
    public void execute() throws IOException {

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

            int reglasActivasParaClase = 0;

            for (int nClases = 0; nClases < train.getnClasses() - 1; nClases++) {
                //Obtain the number of aleatory rule 
                nowfilm = selectClass();
                reglasActivasParaClase = arrayofClass.get(nowfilm).size();
                while (reglasActivasParaClase != 0) {
                    rulesActivated = rulesActivate(arrayofClass.get(nowfilm), arrayReglas);

                    //Obtain de best rule for that example

                    bestRuleObtained = obtainBestRule(rulesActivated.get(rnd.nextInt(rulesActivated.size())));
                    reglasActivasParaClase = deleteRuleCovert(nowfilm, bestRuleObtained);
                    imprimirReglaGenerada(bestRuleObtained, nowfilm);                 
                    //TODO: ATRIS - Eliminar aquellos ejemplos cubiertos por la mejor regla obtenida anteriormente.
                }
                arrayClases[nowfilm] = 0;
            }

        }
    }

    /**
     * Prinf of rule
     *
     * @param rule
     * @param clase
     */
    private void imprimirReglaGenerada(int[] rule, int clase) {
        System.out.println("");
        for (int i = 0; i < rule.length; i++) {
            if(i!= rule.length-1)
                System.out.print(rule[i] + ", ");
            else
                System.out.print(rule[i]);
        }
        System.out.print(" - Clase: " + clase);
    }

    /**
     * Delete rules cover for a out rule
     *
     * @param clase
     * @param rule
     * @return
     */
    private int deleteRuleCovert(int clase, int[] rule) {
        double[][] arrayValores = train.getX();
        boolean salir = false;
        int cantidadClase = 0;
        int sumaClase = 0;

        for (int i = 0; i < arrayReglas.length; i++) {
//            System.out.println("Valor: ");
//            for(int j=0;j< train.getnInputs();j++){
//                System.out.print( arrayValores[i][j]);
//            }
            sumaClase = 0;
            for (int k = 0; k < train.getnInputs(); k++) {
                if (rule[sumaClase + (int) arrayValores[i][k]] != 1) {
                    salir = true;
                }
                sumaClase += valoresAtributo.get(k);
            }
            if (!salir) {
                arrayReglas[i] = 0; //Desactivamos la regla
                //System.out.println("Entra en eliminar: " + i);
            } else {
                salir = false;
            }

        }
        return rulesActivate(arrayofClass.get(nowfilm), arrayReglas).size();
    }

    /**
     * Select the class with less number of examples
     *
     * @return
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
     * It generates the output file from a given dataset and stores it in a file
     *
     * @param dataset myDataset input dataset
     * @param filename String the name of the file
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

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

    private int rulesClassesActivated() {
        int suma = 0;
        for (int i = 0; i < arrayofClass.get(nowfilm).size(); i++) {
            if (rulesActivated.get(arrayofClass.get(nowfilm).get(i)) == 1) {
                suma++;
            }
        }
        return suma;

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

    private int PositivcontainCoverVars(int[] rule, int numberRule) {
        int[] vectorNumerico = new int[train.getnInputs()];
        boolean entra = true;
        int suma_Positivos = 0;

        for (int i = 0; i < arrayReglas.length; i++) {
            entra = true;
            if (arrayReglas[i] == 1) {
                for (int j = 0; j < train.getnInputs(); j++) {
                    if (rule[(int) train.getX()[i][j]] != 1) {
                        entra = false;
                    }
                    break;
                }
                if (entra) {
                    if (train.getOutputAsInteger(i) == train.getOutputAsInteger(numberRule)) {
                        suma_Positivos++;
                    }
                }
            }
        }
        return suma_Positivos;
    }

    private int NegativcontainCoverVars(int[] rule, int numberRule) {
        int[] vectorNumerico = new int[train.getnInputs()];
        boolean entra = true;
        int suma_Negativos = 0;

        for (int i = 0; i < arrayReglas.length; i++) {
            entra = true;
            if (arrayReglas[i] == 1) {
                for (int j = 0; j < train.getnInputs(); j++) {
                    if (rule[(int) train.getX()[i][j]] != 1) {
                        entra = false;
                    }
                    break;
                }
                if (entra) {
                    if (train.getOutputAsInteger(i) != train.getOutputAsInteger(numberRule)) {
                        suma_Negativos++;
                    }
                }
            }
        }
        return suma_Negativos;
    }

    /**
     *
     * @param rule
     * @return
     */
    private int containVars() {
        /*
         * int[] vectorNumerico = new int[train.getnInputs()]; boolean entra =
         * true; int suma_Positivos = 0;
         *
         * for (int i = 0; i < arrayReglas.length; i++) { entra = true; if
         * (arrayReglas[i] == 1) { for (int j = 0; j < train.getnInputs(); j++)
         * { if (rule[(int) train.getX()[i][j]] != 1) { entra = false; } break;
         * } if (entra) { suma_Positivos++; } } }
         */
        return numRuleActivate();
    }

    private int numRuleActivate() {
        int suma = 0;
        for (int i = 0; i < arrayReglas.length; i++) {
            if (arrayReglas[i] == 1) {
                suma++;
            }
        }
        return suma;
    }

    private double mestimate(int[] rule, int numberRule) {
        int P = containVars();
        int N = numRuleActivate() - P;
        int m = 2;
        int p = PositivcontainCoverVars(rule, numberRule);
        int n = NegativcontainCoverVars(rule, numberRule);

        return (p + m * (P / (P + N))) / (p + n + m);


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

    private List<String> obtencionKnn(int cant) {

        int j = 0;
        String ci, cj;
        ArrayList<String> lista = new ArrayList<String>();
        for (int i = 0; i < cant; i++) {
            j = i;
            while (j < train.getnInputs()) {
                ci = String.valueOf(i);
                cj = String.valueOf(j);
                ci.concat(",cj");
                lista.add(ci);
            }
        }
        return lista;
    }

    /**
     *
     * @return
     */
    private int[] _2opt(int[] vectorSalida) {

        Randomize rnd = new Randomize();
        int valoraleat = Randomize.Randint(0, vectorKnn.size());
        String v = vectorKnn.get(valoraleat);
        vectorKnn.remove(valoraleat);

        vecinos = v.split(",");

        //Intercambio de valores
//        int valorInter = vectorSalida[Integer.valueOf(vecinos[0])];
//        vectorSalida[Integer.valueOf(vecinos[0])] = vectorSalida[Integer.valueOf(vecinos[1])];
//        vectorSalida[Integer.valueOf(vecinos[1])] = valorInter;


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

    public int[] obtainBestRule(int example) {

        double[][] arrayValores = train.getX();
        int[] valores = new int[train.getOutputAsInteger().length];
        double bestError = 0, resultado = 9999;
        boolean nuevaRegla = true, acabado = false;

        //Preparate for transladation to Binary number
        for (int i = 0; i < train.getnInputs(); i++) {
            for (int j = 0; j < train.getOutputAsInteger().length; j++) {
                valores[j] = ((int) arrayValores[j][i]);
            }
            Arrays.sort(valores);

            System.out.println("Valores maximos: " + valores[(valores.length - 1)]);
            sizeVectorRule += valores[(valores.length - 1)] + 1;
            valoresAtributo.add(valores[(valores.length - 1)] + 1);
        }

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
            int suma = 0;
            binaryRule[(int) arrayValores[example][0]] = 1;
            for (int j = 1; j < train.getnInputs(); j++) {
                suma += valoresAtributo.get(j - 1);
                binaryRule[suma + (int) arrayValores[example][j]] = 1;
            }
        }

        while (acabado != true) {
            //Calculamos el kNN
            vectorKnn = new ArrayList<String>(obtencionKnn(sizeVectorRule));

            //Calculate the 2-opt
            int i = 0;
            while (i < (sizeVectorRule * (sizeVectorRule - 1) / 2)) {
                int[] ruleVecina = new int[sizeVectorRule];
                ruleVecina = _2opt(binaryRule);

                resultado = mestimate(ruleVecina, example);
                if (bestError > resultado) {
                    bestError = resultado;
                    vecinoMejor = vecinos;
                    i = 0;
                    binaryRule = ruleVecina;
                    nuevaRegla = false;
                    break;
                }
            }

            if (i == (sizeVectorRule * (sizeVectorRule - 1) / 2)) {
                acabado = true;
                break;
            }
        }


// Comentario para mostrar datos por pantalla
//        System.out.println("Ejemplo: " + example);
//        
//            System.out.println("Regla: ");       
//        for(int i =0;i < sizeVectorRule;i++){
// 
//            System.out.print(binaryRule[i] + ", ");
//        }



        return binaryRule;
    }

    /**
     * It launches the algorithm
     */
    public void execute() {

        int[] bestRuleObtained;
        Random rnd = new Random(234);

        if (somethingWrong) { //We do not execute the program
            System.err.println("An error was found, either the data-set have numerical values or missing values.");
            System.err.println("Aborting the program");
            //We should not use the statement: System.exit(-1);
        } else {
            //Comprobaciones a realizar 
            if (train.hasNumericalAttributes()) {
                System.out.println("Se trata de atributos numericos");
            } else if (train.hasRealAttributes()) {
                System.out.println("Se trata de atributos reales");
            } else {
                System.out.println("Se trata de otros tipos de atributo");
            }


            //Determination vector of different Rules
            System.out.println("La clase seleccionada es: " + selectClassInitial());
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

            for (int i = 0; i < arrayofClass.get(0).size(); i++) {
                System.out.println("Valor: " + arrayofClass.get(0).get(i));
            }
            int reglasActivasParaClase = 0;;
            System.out.println("Clases: " + train.getnClasses());
            for (int nClases = 0; nClases < train.getnClasses() - 1; nClases++) {
                //Obtain the number of aleatory rule 
                nowfilm = selectClassInitial();
                reglasActivasParaClase = arrayofClass.get(nowfilm).size();
                while (reglasActivasParaClase != 0) {
                    rulesActivated = rulesActivate(arrayofClass.get(nowfilm), arrayReglas);

                    //Obtain de best rule for that example

                    bestRuleObtained = obtainBestRule(rulesActivated.get(rnd.nextInt(rulesActivated.size())));
                    reglasActivasParaClase = deleteRuleCovert(nowfilm, bestRuleObtained);
                    imprimirReglaGenerada(bestRuleObtained,nowfilm);
                    //TODO: ATRIS - Eliminar aquellos ejemplos cubiertos por la mejor regla obtenida anteriormente.
                }
                arrayClases[nowfilm] = 0;
            }

        }
    }
    
    
    private void imprimirReglaGenerada(int[] rule, int clase){
        System.out.println("");
        for(int i=0; i< rule.length;i++){
            System.out.print(rule[i]+", ");
        }
        System.out.print(" - Clase: " + clase );
    }
    
    private int deleteRuleCovert(int clase, int[] rule) {
        double[][] arrayValores = train.getX();
        boolean salir = false;
        int cantidadClase = 0;
        for (int i = 0; i < rulesClassesActivated(); i++) {
            for (int k = 0; k < train.getnInputs(); k++) {
                if (rule[(int) arrayValores[i][k]] != 1) {
                    salir = true;
                }
            }
            if (!salir) {
                arrayReglas[i] = 0; //Desactivamos la regla;
            } else {
                salir = false;
            }
            cantidadClase++;
        }
        return cantidadClase;
    }

    private int selectClassInitial() {

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

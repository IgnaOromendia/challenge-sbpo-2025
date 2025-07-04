## Tareas pendientes

- Analizar cuántos pasillos usa la solución óptima de los distintos casos, y ver que forma tienen: cantidad de elementos, cantidad de elementos distintos, cantidad de ordenes que puede satisfacer cada pasillo.
- Qué tan rápido anda todo para una cantidad fija de pasillos?
- Capaz eliminar simetrias agregando variables no binaria (si hay k copias de la orden o, entonces poner una variable de 0 ak)


## Tareas Hechas
- Ver pares de pasillos contenidos **Igna: Hecho!**
- Testear todo, ahora que el Divide anda bien **Hecho!**.
- Poner GAP como parametro en parametrico. Usar gap 0 en la ultima iteracion **Hecho!**.
- Pensar heuristicas que trabajen a partir de las soluciones del modelo. **Lu e Igna** 
- Correr Dataset B idem, para hacer los ground truths **Igna: Hecho!**
- Factorizar en archivos distintos: Parametric, Binary, CPLEX runner, Greedy **Igna: Hecho!**
- Iniciar cada iteracion del parametrico con el resultado de la iteracion anterior. **Igna: Hecho!**
- Jugar con los parámetros de Simplex **Igna: Hecho!**
- Correr Dataset A con precision e-4 **Igna: Hecho!**
- Potencialmente romper simetrías (ordenes iguales) unificandolas en una sola variable o dando un orden en el cual agregarlas a la solución **No hay tales**
- Revisar esto https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6858622 **Igna: Hecho!**
- Que el parametrico use ALGO de todo lo que hicimos (cota inferior greedy, ) **Igna: Hecho!**
- En el run_solver agregar que se corra el checker.py contra las soluciones. **Cifu: Hecho!**
- Hacer un algoritmo greedy para intentar encontrar una solución inicial que sea mejor que la que da Simplex con k=0. Una idea es tomar pasillos grandes y recorrer las ordenes de mas grandes a mas chicas intentando colocarlas. **Cifu: Hecho!**
- Cuando se resuelve el problema $\max(N(x) - qD(x))$ una cota superior $B$ a este problema se traduce a una cota al problema fraccional como $\frac{N(x)}{D(x)} \leq \frac{B}{D(x)} + q \leq B + q$. Usar.
- Formato de salida: hay que imprimir las órdenes y pasillos en orden?
- Resolver la relajación lineal para encontrar una cota superior. Hay que implementar el método de Cooper. **Igna: Hecho!**
- La binaria no puede resolver el llamado a simplex en instancias grandes donde el k está muy cerca del valor óptimo real. Considerar no usar la binaria en general (solo hacer paramétrico) o en particular (hacer binaria hasta cierto punto). **Igna: Hecho!**
- Hacer un tester del modelo: un conjunto de instancias chicas de las cuales sepamos la solución, y un bash que corrar el código completo y verifique si las soluciones son correctas y alcanzan el óptimo. Entre estas instancias debería haber casos borde. **Santi: Hecho!**
- Refactorizar el código. **Igna: Hecho!**
- Poner función de optimización en la binaria: ahora mismo estamos pidiendo que $\sum_{ordenes} \sum_{e en orden} e \geq k |A|$ donde $A$ es el conjunto de pasillos elegidos, y no estamos optimizando nada. Podemos pedir maximizar $\sum_{o \in ordenes} \sum_{e \in o} e - k |A|$ (incluso podemos sacar la restricción de que sea mayor a cero). Hay que ver cómo empeora la performance si optimizamos en vez de pedir factibilidad. **Igna: Hecho!**
- Mejorar las cotas superiores e inferiores: **Luciana: Hecho!**
  - Como inferior se puede usar el resultado de la primera corrida. Alternativamente podemos usar alguna solución golosa.
  - Como superior podemos usar la suma de los elementos. También podemos acotra a mano el caso de un solo pasillo, y luego acotar lo que queda por UB / 2. La cota de un pasillo es directamente el tamaño del pasillo.

## Cómo se hace un benchmark

Se correr todos los casos, con tolerancia $10^{-3}$.

## Resultados actuales

El objetivo es procesar todas las instancias en 10 minutos encontrando los óptimos (módulo alguna precisión epsilon)

| Fecha       | Tiempo en procesar todo  |
|-------------|------------------------  |
|             |                          |        

## Ejemplos de cómo correr las cosas

1. Compile and run benchmarks:
    ```sh
    python run_challenge.py src/main/java/org/sbpo2025/challenge src/main/resources/instances output
    ```
   
2. Check solution viability:
    ```sh
    python checker.py src/main/resources/instances/instance_001.txt output/instance_001.txt
    ```

## Testing

En ```tests/``` hay un conjunto de instancias para las cuales sabemos la solución. Para comparar los resultados del modelo actual con esos, correr

    bash test_solver.sh

El script reporta errores y trata de dar un motivo asociado al mismo.

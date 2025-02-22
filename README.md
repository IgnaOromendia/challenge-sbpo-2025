## Tareas pendientes

- Hacer un tester del modelo: un conjunto de instancias chicas de las cuales sepamos la solución, y un bash que corrar el código completo y verifique si las soluciones son correctas y alcanzan el óptimo. Entre estas instancias debería haber casos borde. **Santi: Hecho!**
- Refactorizar el código. **Igna**
- Poner función de optimización en la binaria: ahora mismo estamos pidiendo que $\sum_{ordenes} \sum_{e en orden} e \geq k |A|$ donde $A$ es el conjunto de pasillos elegidos, y no estamos optimizando nada. Podemos pedir maximizar $\sum_{o \in ordenes} \sum_{e \in o} e - k |A|$ (incluso podemos sacar la restricción de que sea mayor a cero). Hay que ver cómo empeora la performance si optimizamos en vez de pedir factibilidad. **Igna**
- Mejorar las cotas superiores e inferiores: **Luciana**
  - Como inferior se puede usar el resultado de la primera corrida. Alternativamente podemos usar alguna solución golosa.
  - Como superior podemos usar la suma de los elementos. También podemos acotra a mano el caso de un solo pasillo, y luego acotar lo que queda por UB / 2. La cota de un pasillo es directamente el tamaño del pasillo. En general, odernar los pasillos por  
- Que la binaria vaya dividiendo por 4 hasta llegar a un rango factible. **a futuro**
- Revisar esto https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6858622 **a futuro**
- Jugar con los parámetros de Simplex **a futuro**
- Formato de salida: hay que imprimir las órdenes y pasillos en orden?

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
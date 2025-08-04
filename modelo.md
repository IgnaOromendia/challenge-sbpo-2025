$X_i$ representa elegir la orden $i$\
$Y_j$ representa elegir el pasillo $j$

$$
\begin{matrix}
\max & \sum_{o = 0}^{|O|} \sum_{i=0}^n X_ou_{oi} - q \sum_{a=0}^{|A|} Y_a \\ \\
s.t. & \sum_{o=0}^{|O|} X_ou_{oi} \leq \sum_{a=0}^n Y_au_{ai} & \forall i = 0\dots n \\ \\
& \sum_{o = 0}^{|O|}\sum_{i=0}^n X_iu_{io} \geq \text{LB}  \\ \\
& \sum_{o = 0}^{|O|}\sum_{i=0}^n X_iu_{io} \leq \text{UB} \\ \\
& \sum_{o = 0}^{|O|}\sum_{i=0}^n X_iu_{io} \leq k \sum_{j=0}^{|A|} Y_j\\ \\
& \sum_{j=0}^{|A|} Y_j \geq 1 \\ \\
& X_i \in \{0,1\} & \forall i=0\dots |O| \\ \\ 
& Y_i \in \{0,1\} & \forall i=0\dots |A|
\end{matrix}
$$



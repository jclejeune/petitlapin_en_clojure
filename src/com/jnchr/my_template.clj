(ns com.jnchr.my-template
  (:import [java.awt.event KeyEvent ActionListener]
           [javax.swing JFrame JPanel Timer]
           [java.awt Color Font]))

;; Dimensions de la grille
(def cell-size 50) ;; Taille d'une case
(def grid-width 7) ;; 7 cases en largeur
(def grid-height 11) ;; 11 cases en hauteur

;; Dimensions de la zone de jeu
(def zone-width (* cell-size grid-width)) ;; 350px
(def zone-height (* cell-size grid-height)) ;; 550px

;; Positions initiales des entités
(defonce miam (atom nil))               ;; Position du miam (magenta) (stockée comme map)
(defonce miam-alive? (atom true))       ;; Indicateur d'apparition du miam
(defonce circle-pos (atom {:x 3 :y 10})) ;; Lapin (jaune)
(defonce enemy-pos (atom {:x 3 :y 0}))     ;; Renard (cyan-orange)
(defonce game-over (atom false))           ;; Indicateur de fin de partie
(def circle-color (atom Color/YELLOW)) ;; Couleur du lapin
(def enemy-color (atom Color/CYAN))     ;; Couleur du renard


;; Score et hi-score
(def score (atom 0))
(def hi-score (atom 0))

;; Définition des murs (1 = mur, 0 = vide)
(def walls
  [[0 0 0 0 0 0 0]
   [0 1 0 1 0 1 0]
   [0 0 0 1 0 0 0]
   [0 1 0 0 0 1 0]
   [0 0 0 1 0 0 0]
   [0 1 0 0 0 1 0]
   [0 0 0 1 0 0 0]
   [0 1 0 0 0 1 0]
   [0 0 0 1 0 0 0]
   [0 1 0 1 0 1 0]
   [0 0 0 0 0 0 0]])

;; Fonction pour dessiner la grille et le fond
(defn draw-grid [g]
  (.setColor g Color/BLACK)
  (.fillRect g 0 0 zone-width zone-height)
  (.setColor g (Color. 50 50 50))
  (.fillRect g 0 0 zone-width zone-height)
  (.setColor g (Color. 221 46 68))
  (doseq [y (range grid-height) x (range grid-width)]
    (when (= 1 (get-in walls [y x]))
      (.fillRect g (* x cell-size) (* y cell-size) cell-size cell-size)))
  (.setColor g Color/BLACK)
  (dotimes [i (inc grid-width)] (.drawLine g (* i cell-size) 0 (* i cell-size) zone-height))
  (dotimes [j (inc grid-height)] (.drawLine g 0 (* j cell-size) zone-width (* j cell-size))))

;; Fonction pour dessiner un cercle
(defn draw-circle [g pos color]
  (let [{:keys [x y]} @pos]
    (.setColor g color)
    (.fillOval g (+ (* x cell-size) 10) (+ (* y cell-size) 10) 30 30)))

;; Fonction pour générer une position aléatoire pour le miam (stocké en map)
(defn spawn-miam []
  (let [empty-positions (for [y (range grid-height)
                              x (range grid-width)
                              :when (and (= 0 (get-in walls [y x]))
                                         (not= {:x x :y y} @circle-pos)
                                         (not= {:x x :y y} @enemy-pos))]
                          {:x x :y y})
        new-pos (when (seq empty-positions) (rand-nth empty-positions))]
    (when new-pos
      (reset! miam new-pos))))

;; Fonction pour dessiner le miam
(defn draw-miam [g]
  (when @miam-alive?
    (when (nil? @miam)
      (spawn-miam))
    (when @miam
      (let [{:keys [x y]} @miam]
        (.setColor g Color/MAGENTA)
        (.fillOval g (+ (* x cell-size) 10)
                   (+ (* y cell-size) 10)
                   30 30)))))

;; Panel pour afficher la zone de jeu et les entités
(defn CirclePanel []
  (proxy [JPanel] []
    (paintComponent [g]
      (proxy-super paintComponent g)
      (draw-grid g)
      (draw-miam g)                                  ;; Dessiner le miam en premier
      (draw-circle g circle-pos @circle-color)       ;; Dessiner le lapin ensuite
      (draw-circle g enemy-pos @enemy-color)         ;; Dessiner le renard en dernier (affiché en priorité)
      (when @game-over
        ;; Dessiner l'overlay de fin de partie
        (.setColor g (Color. 100 100 100 150))
        (.fillRect g 0 0 zone-width zone-height)
        (let [center-x (/ zone-width 2)
              center-y (/ zone-height 2)]
          (.setFont g (Font. "Arial" Font/BOLD 36))
          (.setColor g Color/RED)
          (.drawString g "GAME OVER" (- center-x 100) (- center-y 60))
          (.setColor g (Color. 173 216 230)) ;; bleu clair
          (.drawString g (str "Score: " @score) (- center-x 80) center-y)
          (when (> @score @hi-score)
            (.setColor g Color/YELLOW)
            (.drawString g (str "Hi-Score: " @hi-score) (- center-x 80) (+ center-y 60))))))))


;; Vérifier si un déplacement est possible
(defn can-move? [x y]
  (and (>= x 0) (< x grid-width)
       (>= y 0) (< y grid-height)
       (= 0 (get-in walls [y x]))))

;; Déplacer le lapin
(defn move-circle [dx dy]
  (when-not @game-over
    (let [{:keys [x y]} @circle-pos
          new-x (+ x dx)
          new-y (+ y dy)]
      (when (can-move? new-x new-y)
        (reset! circle-pos {:x new-x :y new-y})))))

(defn handle-key-press [e]
  (let [key-code (.getKeyCode e)]
    (cond
      (and (= key-code KeyEvent/VK_SPACE) @game-over)
      (do
        ;; Redémarrer la partie avec SPACE en cas de game over
        (reset! game-over false)
        (reset! miam nil)
        (reset! miam-alive? true)
        (reset! circle-pos {:x 3 :y 10})
        (reset! enemy-pos {:x 3 :y 0})
        (reset! score 0)
        (.repaint (.getSource e)))
      (= key-code KeyEvent/VK_LEFT)  (move-circle -1 0)
      (= key-code KeyEvent/VK_RIGHT) (move-circle 1 0)
      (= key-code KeyEvent/VK_UP)    (move-circle 0 -1)
      (= key-code KeyEvent/VK_DOWN)  (move-circle 0 1)

      :else nil)
    (.repaint (.getSource e))))

;; Calcul de la distance euclidienne
(defn distance [x1 y1 x2 y2]
  (Math/sqrt (+ (Math/pow (- x1 x2) 2) (Math/pow (- y1 y2) 2))))

;; Vérifier la condition de fin de partie
(defn check-game-over []
  (when (= @circle-pos @enemy-pos)
    (reset! game-over true)
    (when (> @score @hi-score)
      (reset! hi-score @score))
    ;; Le repaint est déclenché dans la boucle de l'ennemi
    ))
;; Déplacer l'ennemi en direction du joueur
(defn move-enemy []
  (when-not @game-over
    (let [{px :x py :y} @circle-pos
          {ex :x ey :y} @enemy-pos
          moves [[-1 0] [1 0] [0 -1] [0 1]]
          valid-moves (filter #(can-move? (+ ex (first %)) (+ ey (second %))) moves)
          best-move (apply min-key (fn [[dx dy]]
                                     (distance (+ ex dx) (+ ey dy) px py))
                           valid-moves)]
      (when best-move
        (reset! enemy-pos {:x (+ ex (first best-move))
                           :y (+ ey (second best-move))})))
    (check-game-over)))
;; Lorsqu'un miam est mangé : ajouter 50 points, le faire disparaître et le faire réapparaître après un délai
(defn check-for-miam [panel]
  (when (and @miam-alive? @miam (= @circle-pos @miam))
    (swap! score + 50)
    (reset! miam nil)        ;; Le miam disparaît
    (reset! miam-alive? false)
    (.repaint panel)
    (future
      (Thread/sleep (+ 1600 (rand-int 801)))  ;; Délai aléatoire entre 1600 et 2400 ms
      (spawn-miam)  ;; Le miam réapparaît à un nouvel endroit
      (reset! miam-alive? true)
      (.repaint panel))))
;; Boucle de l'ennemi toutes les 300ms
(defn start-enemy-loop [panel]
  (let [action-listener (proxy [ActionListener] []
                          (actionPerformed [e]
                            (move-enemy)
                            (check-for-miam panel)
                            (.repaint panel)))]
    (doto (Timer. 300 action-listener)
      (.start))))

(defn -main []
  (let [frame (JFrame. "Déplacement du cercle avec Murs")
        panel (CirclePanel)]
    (.setSize frame zone-width zone-height)
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.add frame panel)
    (.setFocusable panel true)
    (.addKeyListener panel (proxy [java.awt.event.KeyListener] []
                             (keyPressed [e] (handle-key-press e))
                             (keyReleased [e] nil)
                             (keyTyped [e] nil)))
    (.setVisible frame true)
    (start-enemy-loop panel)))

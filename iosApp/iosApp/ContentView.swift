import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var workoutViewModel = WorkoutViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Wolf Header
                HStack {
                    Image(systemName: "pawprint.fill")
                        .foregroundColor(.blue)
                        .font(.title)
                    Text("Fortis Lupus")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    Image(systemName: "pawprint.fill")
                        .foregroundColor(.blue)
                        .font(.title)
                }
                .padding()
                
                // Today's Hunt Card
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Today's Hunt")
                            .font(.title2)
                            .fontWeight(.semibold)
                        Spacer()
                        Text(Date(), style: .date)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Button(action: {
                        // Start workout action
                    }) {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("Begin the Hunt")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(16)
                .padding(.horizontal)
                
                // Pack History
                VStack(alignment: .leading, spacing: 12) {
                    Text("Pack History")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .padding(.horizontal)
                    
                    if workoutViewModel.recentWorkouts.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "figure.strengthtraining.functional")
                                .font(.system(size: 60))
                                .foregroundColor(.gray)
                            Text("No hunts yet, Alpha!")
                                .font(.headline)
                                .foregroundColor(.secondary)
                            Text("Start your first workout to begin building your strength.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()
                    } else {
                        // Recent workouts list would go here
                        Text("Recent workouts coming soon...")
                            .foregroundColor(.secondary)
                            .padding()
                    }
                }
                
                Spacer()
            }
            .navigationBarHidden(true)
        }
    }
}

class WorkoutViewModel: ObservableObject {
    @Published var recentWorkouts: [Workout] = []
    
    init() {
        // Initialize with shared KMP business logic
        loadRecentWorkouts()
    }
    
    private func loadRecentWorkouts() {
        // TODO: Connect to shared KMP repository
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
import SwiftUI

struct RegisterView: View {
    @State private var username = ""
    @State private var password = ""
    @State private var errorMessage = ""
    @State private var isLoading = false
    @State private var isRegistered = false

    var body: some View {
        VStack {
            if isRegistered {
                LoginView()
            } else {
                Text("Registrierung")
                    .font(.title)
                    .padding()

                TextField("Benutzername", text: $username)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding()
                    .autocapitalization(.none)
                    .accessibilityLabel("Geben Sie Ihren gewünschten Benutzernamen ein")

                SecureField("Passwort", text: $password)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding()
                    .accessibilityLabel("Geben Sie Ihr Passwort ein")

                if isLoading {
                    ProgressView()
                        .padding()
                        .accessibilityLabel("Lade")
                }

                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding()
                        .accessibilityLabel(errorMessage)
                }

                Button(action: {
                    Task {
                        await register()
                    }
                }) {
                    Text("Registrieren")
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(username.isEmpty || password.isEmpty ? Color.gray : Color.blue)
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .disabled(username.isEmpty || password.isEmpty)
                .padding(.horizontal)
            }
        }
        .navigationTitle("Registrierung")
    }

    func register() async {
        isLoading = true
        errorMessage = ""
        guard let url = URL(string: "http://192.168.1.100:8080/register") else {
            errorMessage = "Ungültige URL"
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        let body = "username=\(username.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")&password=\(password.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")&role=POLICE"
        request.httpBody = body.data(using: .utf8)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                DispatchQueue.main.async {
                    isRegistered = true
                    isLoading = false
                }
            } else {
                let errorText = String(data: data, encoding: .utf8) ?? "Unbekannter Fehler"
                DispatchQueue.main.async {
                    errorMessage = "Registrierung fehlgeschlagen: \(errorText)"
                    isLoading = false
                }
            }
        } catch {
            DispatchQueue.main.async {
                errorMessage = "Fehler beim Registrieren: \(error.localizedDescription)"
                isLoading = false
            }
        }
    }
}

struct RegisterView_Previews: PreviewProvider {
    static var previews: some View {
        RegisterView()
    }
}
import SwiftUI

struct LoginView: View {
    @State private var username = ""
    @State private var password = ""
    @State private var errorMessage = ""
    @State private var isLoading = false
    @State private var isAuthenticated = false

    var body: some View {
        NavigationView {
            VStack {
                if isAuthenticated {
                    ContentView()
                } else {
                    Text("Vermisstenmeldungssystem")
                        .font(.title)
                        .padding()

                    TextField("Benutzername", text: $username)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .padding()
                        .autocapitalization(.none)
                        .accessibilityLabel("Geben Sie Ihren Benutzernamen ein")

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
                            await login()
                        }
                    }) {
                        Text("Anmelden")
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(username.isEmpty || password.isEmpty ? Color.gray : Color.blue)
                            .foregroundColor(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                    .disabled(username.isEmpty || password.isEmpty)
                    .padding(.horizontal)

                    NavigationLink("Kein Konto? Hier registrieren", destination: RegisterView())
                        .padding()
                        .accessibilityLabel("Zum Registrieren navigieren")
                }
            }
            .navigationTitle("Anmeldung")
        }
    }

    func login() async {
        isLoading = true
        errorMessage = ""
        guard let url = URL(string: "http://192.168.1.100:8080/login") else {
            errorMessage = "Ungültige URL"
            isLoading = false
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

        let body = "username=\(username.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")&password=\(password.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")"
        request.httpBody = body.data(using: .utf8)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                // Extract JSESSIONID from cookies
                if let cookies = HTTPCookieStorage.shared.cookies(for: url) {
                    let cookieDict = cookies.reduce(into: [String: String]()) { result, cookie in
                        result[cookie.name] = cookie.value
                    }
                    if let jsessionId = cookieDict["JSESSIONID"] {
                        UserDefaults.standard.set(jsessionId, forKey: "JSESSIONID")
                        UserDefaults.standard.set(true, forKey: "isAuthenticated")
                        DispatchQueue.main.async {
                            isAuthenticated = true
                            isLoading = false
                        }
                    } else {
                        DispatchQueue.main.async {
                            errorMessage = "Kein Authentifizierungs-Cookie erhalten"
                            isLoading = false
                        }
                    }
                }
            } else {
                DispatchQueue.main.async {
                    errorMessage = "Ungültiger Benutzername oder Passwort"
                    isLoading = false
                }
            }
        } catch {
            DispatchQueue.main.async {
                errorMessage = "Fehler beim Anmelden: \(error.localizedDescription)"
                isLoading = false
            }
        }
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}